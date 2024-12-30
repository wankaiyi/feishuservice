package com.wky.feishuservice.client;

import com.wky.feishuservice.annotation.TimedExecution;
import com.wky.feishuservice.cache.ChatMsgCache;
import com.wky.feishuservice.config.OpenAiConfig;
import com.wky.feishuservice.constants.OpenAiConstants;
import com.wky.feishuservice.exceptions.OpenAiException;
import com.wky.feishuservice.mapper.UserPromptMapper;
import com.wky.feishuservice.model.bo.ChatResponseBO;
import com.wky.feishuservice.model.dto.ChatRequestDTO;
import com.wky.feishuservice.model.dto.ChatResponseDTO;
import com.wky.feishuservice.model.dto.ImageGenerateRequestDTO;
import com.wky.feishuservice.model.dto.ImageGenerateResponseDTO;
import com.wky.feishuservice.selector.ApiKeySelector;
import com.wky.feishuservice.utils.HttpUtils;
import com.wky.feishuservice.utils.JacksonUtils;
import com.wky.feishuservice.utils.RedisUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 参考<a href="https://openai.apifox.cn/doc-2653713">OpenAI Apifox文档</a>
 *
 * @author wky
 * @date 2024/11/11
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiClient {

    private final ChatMsgCache chatMsgCache;

    private static final String GPT_4_O_MODEL = "gpt-4o";

    private final ApiKeySelector apiKeySelector;

    private static final Map<String, Map<String, String>> HEADER_PARAMS_MAPS = new HashMap<>();
    private final RedisUtils redisUtils;
    private final OpenAiConfig openAiConfig;
    private final UserPromptMapper userPromptMapper;

    @PostConstruct
    public void init() {
        List<String> apiKeys = openAiConfig.getApiKeys();
        apiKeys.forEach(apiKey -> {
            Map<String, String> headerParams = new HashMap<>() {{
                put("Authorization", "Bearer " + apiKey);
                put("Content-Type", "application/json; charset=utf-8");
            }};
            HEADER_PARAMS_MAPS.put(apiKey, headerParams);
        });
    }

    @TimedExecution(methodDescription = "chatgpt对话")
    public ChatResponseBO chat(String openId, String text) {
        String apiKey = apiKeySelector.selectApiKey();
        List<ChatRequestDTO.Message> messages = addMessageToCache(openId, text);
        // 拼接提示词
        addPromptIfNeeded(messages, openId);

        ChatResponseDTO response = processChatgptRequest(messages, apiKey);
        ChatResponseDTO.ChatError error;
        if (Objects.nonNull(error = response.getError())) {
            throw new OpenAiException(String.format("apikey: %s，请求发生错误：%s", desensitizeString(apiKey), error.getMessage()));
        }

        String resContent = response.getChoices()[0].getMessageResp().getContent();

        cacheMsg(openId, resContent);

        BigDecimal price = getPrice(response.getUsage().getPromptTokens(), response.getUsage().getCompletionTokens());
        updateBalance(price, apiKey);

        return new ChatResponseBO()
                .setContent(resContent)
                .setModel(response.getModel())
                .setTotalTokens(response.getUsage().getTotalTokens())
                .setPrice(price);
    }

    private void addPromptIfNeeded(List<ChatRequestDTO.Message> messages, String openId) {
        String prompt = userPromptMapper.selectUserPrompt(openId);
        if (Strings.isNotBlank(prompt)) {
            messages.add(0, new ChatRequestDTO.Message("system", prompt));
        }
        messages.addAll(
                1,
                List.of(
                        new ChatRequestDTO.Message("user", "使用中文回答所有问题"),
                        new ChatRequestDTO.Message("assistant", "好的，了解")
                )
        );
    }

    private void cacheMsg(String openId, String resContent) {
        chatMsgCache.addMsgCache(openId, new ChatRequestDTO.Message()
                .setContent(resContent)
                .setRole("assistant"));
    }

    /**
     * 脱敏字符串，只保留前后5个字符，中间用星号代替
     */
    public static String desensitizeString(String str) {
        // 检查字符串长度是否足够长
        if (str.length() <= 10) {
            // 如果字符串长度不超过10个字符，则不进行脱敏
            return str;
        }

        // 获取开头和结尾各5个字符
        String start = str.substring(0, 5);
        String end = str.substring(str.length() - 5);

        // 中间部分需要的星号数量
        int starsCount = str.length() - 10;

        return start + "*".repeat(starsCount) + end;
    }

    private static ChatResponseDTO processChatgptRequest(List<ChatRequestDTO.Message> messages, String apiKey) {
        ChatRequestDTO chatRequestDTO = new ChatRequestDTO(GPT_4_O_MODEL, messages);
        String data = JacksonUtils.serialize(chatRequestDTO);
        log.info("请求OpenAI接口，请求体：{}", data);
        // 接口超时会导致返回null
        String result = HttpUtils.postForm(OpenAiConstants.CHAT_URL, data, HEADER_PARAMS_MAPS.get(apiKey), new HashMap<>() {{
            put("model", GPT_4_O_MODEL);
        }});
        log.info("OpenAI接口返回结果: {}", result);
        if (Objects.isNull(result)) {
            throw new OpenAiException("openai接口异常，请查看日志");
        }
        return JacksonUtils.deserialize(result, ChatResponseDTO.class);
    }

    private List<ChatRequestDTO.Message> addMessageToCache(String openId, String text) {
        ChatRequestDTO.Message userMsg = new ChatRequestDTO.Message("user", text);
        chatMsgCache.addMsgCache(openId, userMsg);
        return chatMsgCache.getMsgCache(openId);
    }

    private void updateBalance(BigDecimal price, String apiKey) {
        String key = "chatgpt:price:" + apiKey;
        BigDecimal cachedPrice = new BigDecimal(redisUtils.get(key));
        redisUtils.set(key, cachedPrice.subtract(price).toPlainString());
    }

    /**
     * 价格参考 <a href="https://openai.com/api/pricing/">OpenAI pricing</a>
     */
    private static final BigDecimal PRICE_PER_INPUT_TOKEN = BigDecimal.valueOf(2.5 / 1000000.0);
    private static final BigDecimal PRICE_PER_OUTPUT_TOKEN = BigDecimal.valueOf(10 / 1000000.0);

    /**
     * @param promptTokens     输入的token数
     * @param completionTokens 输出的token数
     * @return 价格
     */
    public BigDecimal getPrice(int promptTokens, int completionTokens) {
        return PRICE_PER_INPUT_TOKEN.multiply(BigDecimal.valueOf(promptTokens))
                .add(PRICE_PER_OUTPUT_TOKEN.multiply(BigDecimal.valueOf(completionTokens)));
    }

    public ImageGenerateResponseDTO generateImage(String prompt) {
        ImageGenerateRequestDTO imageGenerateRequestDTO = new ImageGenerateRequestDTO()
                .setPrompt(prompt);
        String result = HttpUtils.postForm(
                OpenAiConstants.IMAGE_GENERATION_URL,
                JacksonUtils.serialize(imageGenerateRequestDTO),
                HEADER_PARAMS_MAPS.get(apiKeySelector.selectApiKey())
        );
        return JacksonUtils.deserialize(result, ImageGenerateResponseDTO.class);
    }

    @TimedExecution(methodDescription = "根据用户问题提示下一步用户可能问出的问题")
    public ChatResponseBO getPredictNextQuestion(String openId, String question) {
        String apiKey = apiKeySelector.selectApiKey();
        List<ChatRequestDTO.Message> messages = new ArrayList<>();
        messages.add(new ChatRequestDTO.Message("user", question));
        // 拼接提示词
        addPromptIfNeeded(messages, openId);
        ChatResponseDTO response = processChatgptRequest(
                List.of(new ChatRequestDTO.Message("system", OpenAiConstants.PREDICT_QUESTION_PROMPT),
                        new ChatRequestDTO.Message("user", JacksonUtils.serialize(messages) + "根据这个上下文预测用户下一个问题"))
                , apiKey);
        ChatResponseDTO.ChatError error;
        if (Objects.nonNull(error = response.getError())) {
            throw new OpenAiException(String.format("apikey: %s，请求发生错误：%s", desensitizeString(apiKey), error.getMessage()));
        }

        String resContent = response.getChoices()[0].getMessageResp().getContent();

        BigDecimal price = getPrice(response.getUsage().getPromptTokens(), response.getUsage().getCompletionTokens());
        updateBalance(price, apiKey);

        return new ChatResponseBO()
                .setContent(resContent)
                .setModel(response.getModel())
                .setTotalTokens(response.getUsage().getTotalTokens())
                .setPrice(price);
    }
}
