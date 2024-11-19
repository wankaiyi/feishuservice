package com.wky.feishuservice.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * jackson序列化/反序列化工具类
 */
@Slf4j
public class JacksonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.findAndRegisterModules();
        Set<Object> registeredModuleIds = MAPPER.getRegisteredModuleIds();
        String registeredModuleClassNames = registeredModuleIds.stream().map(Object::toString).collect(Collectors.joining(","));
        log.info("JacksonUtils findAndRegisterModules size {} classNames {} ", registeredModuleIds.size(), registeredModuleClassNames);
        MAPPER.registerModule(new Jdk8Module()); // 出现过丢失 这里强制补充一下
        MAPPER.registerModule(new JavaTimeModule()); // 出现过丢失 这里强制补充一下
        MAPPER.registerModule(new ParameterNamesModule()); // 出现过丢失 这里强制补充一下
    }

    /**
     * 将json结果集转化为对象
     *
     * @param json     json字符串
     * @param beanType 对象中的object类型
     */
    public static <T> T deserialize(String json, Class<T> beanType) {
        T object = null;
        try {
            object = MAPPER.readValue(json, beanType);
        } catch (Exception e) {
            log.error("Jackson反序列化失败，json: {}, beanType: {}", json, beanType.getSimpleName(), e);
        }
        return object;
    }

    /**
     * 将json转换成pojo对象list
     *
     * @param json     json字符串
     * @param beanType 反序列化目标类型class
     * @param <T>      反序列化目标类型
     * @return 反序列化后的对象
     */
    public static <T> List<T> deserializeList(String json, Class<T> beanType) {
        JavaType javaType = MAPPER.getTypeFactory().constructParametricType(List.class, beanType);
        List<T> objectList = Collections.emptyList();
        try {
            objectList = MAPPER.readValue(json, javaType);
        } catch (Exception e) {
            log.error("Jackson反序列化失败，jsonList: {}, beanType: {}", json, beanType.getSimpleName(), e);
        }
        return objectList;
    }

    /**
     * 序列化pojo
     *
     * @param object pojo
     * @param <T>    pojo类型
     * @return 对象序列化后的json字符串
     */
    public static <T> String serialize(T object) {
        String json = null;
        try {
            json = MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            log.error("Jackson序列化失败", e);
        }
        return json;
    }

    /**
     * 把ObjectNode转换成指定类型
     *
     * @param node     源node
     * @param beanType 目标pojo类型class
     * @param <T>      pojo类型
     * @return 目标pojo
     */
    public static <T> T convert(ObjectNode node, Class<T> beanType) {
        T t = null;
        try {
            t = MAPPER.treeToValue(node, beanType);
        } catch (JsonProcessingException e) {
            log.error("Jackson ObjectNode转换失败", e);
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    public static <T> T jsonCopy(T old) {
        return deserialize(serialize(old), (Class<T>) old.getClass());
    }
}

