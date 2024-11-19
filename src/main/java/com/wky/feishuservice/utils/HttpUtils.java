package com.wky.feishuservice.utils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.apache.commons.lang3.ObjectUtils;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;


/**
 * http client utils
 * 外部用完 ResponseBody body = Response.body() ，body 需要手动关闭
 */
@Slf4j
public class HttpUtils {

    public static final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded");
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final int DEFAULT_TIME_LENGTH = 1000;
    public final static ConnectionPool defaultConnectionPool = new ConnectionPool(20, 5, TimeUnit.MINUTES);
    public static OkHttpClient okHttpClientFinal = new OkHttpClient();
    public static OkHttpClient okHttpClient = okHttpClientFinal.newBuilder()
            .connectionPool(defaultConnectionPool)
            .connectTimeout(40 * DEFAULT_TIME_LENGTH, TimeUnit.MILLISECONDS)
            .writeTimeout(60 * DEFAULT_TIME_LENGTH, TimeUnit.MILLISECONDS)
            .readTimeout(60 * DEFAULT_TIME_LENGTH, TimeUnit.MILLISECONDS)
            .build();


    //有些耗时过长的链接
    public static OkHttpClient okHttpClientLong = okHttpClientFinal.newBuilder()
            .connectionPool(defaultConnectionPool)
            .connectTimeout(80 * DEFAULT_TIME_LENGTH, TimeUnit.MILLISECONDS)
            .writeTimeout(60 * DEFAULT_TIME_LENGTH, TimeUnit.MILLISECONDS)
            .readTimeout(60 * DEFAULT_TIME_LENGTH, TimeUnit.MILLISECONDS)
            .build();

    public static ResponseBody execute(Request.Builder requestBuilder) {
        return execute(okHttpClient, requestBuilder);
    }

    public static ResponseBody executeLong(Request.Builder requestBuilder) {
        return execute(okHttpClientLong, requestBuilder);
    }

    public static ResponseBody execute(OkHttpClient client, Request.Builder requestBuilder) {
        Request request = requestBuilder.build();
        return execute(client, request);
    }

    /**
     *
     */
    public static ResponseBody execute(OkHttpClient client, Request request) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("request:{}, header:{}", request, request.headers());
            }
            Response response = client.newCall(request).execute();
            if (log.isDebugEnabled()) {
                log.debug("response:{}", response);
            }
            ResponseBody body = response.body();
            if (ObjectUtils.isEmpty(body)) {
                return null;
            }
            return body;
        } catch (SocketTimeoutException e) {
            log.error("socket time out exception，url={}", request.url().host(), e);
            return null;
        } catch (IOException e) {
            log.error("http throw IOException", e);
            return null;
        }
    }

    public static Response executeRequest(Request request) {
        String requestString = null;
        Buffer requestBuffer = new Buffer();

        try {
            RequestBody body = request.body();
            if (Objects.nonNull(body)) {
                body.writeTo(requestBuffer);
                requestString = requestBuffer.readUtf8();
            }
            return okHttpClient.newCall(request).execute();
        } catch (SocketTimeoutException e) {
            log.error("socket time out exception, param={}, url={}", request.url().host(), requestString);
            return null;
        } catch (IOException e) {
            log.error("http throw IOException, param={}", requestString);
            return null;
        }
    }

    /**
     * 解析响应体
     *
     * @param responseBody
     * @return
     */
    public static String doParseResponseBody(ResponseBody responseBody) {
        try {
            if (ObjectUtils.isNotEmpty(responseBody)) {
                // string() 会自动关闭资源
                String response = responseBody.string();
                if (log.isDebugEnabled()) {
                    log.debug("response body:{}", response);
                }
                return ObjectUtils.isEmpty(response) ? null : response.trim();
            }
        } catch (Throwable th) {
            log.error("http请求调用失败 异常", th);
            throw new RuntimeException(th.getMessage());
        }
        return null;
    }

    /**
     * GET 请求
     *
     * @param url 请求url
     */
    public static String get(String url) {
        return get(url, null, null);
    }

    /**
     * GET 请求
     *
     * @param url           请求url
     * @param requestParams 请求参数
     */
    public static String get(String url, Map<String, String> requestParams) {
        return get(url, requestParams, null);
    }

    /**
     * GET 请求
     *
     * @param url           请求url
     * @param requestParams 请求参数
     * @param headersParams 请求头参数
     */
    public static String get(String url, Map<String, String> requestParams, Map<String, String> headersParams) {
        return doParseResponseBody(getResponseBody(url, requestParams, headersParams));
    }


    /**
     * GET 请求
     *
     * @param url 请求url
     */
    public static ResponseBody getResponseBody(String url) {
        HttpUrl.Builder httpUrlBuild = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        Request.Builder builder = new Request.Builder();
        builder.get();
        builder.url(httpUrlBuild.build());
        return execute(builder);
    }

    /**
     * GET 请求
     *
     * @param url           请求url
     * @param requestParams 请求参数
     * @param headersParams 请求头参数
     */
    public static ResponseBody getResponseBody(String url, Map<String, String> requestParams, Map<String, String> headersParams) {
        HttpUrl.Builder httpUrlBuild = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        if (ObjectUtils.isNotEmpty(requestParams)) {
            requestParams.forEach(httpUrlBuild::addQueryParameter);
        }
        Request.Builder builder = new Request.Builder();
        if (ObjectUtils.isNotEmpty(headersParams)) {
            builder.headers(setHeaders(headersParams));
        }
        builder.get();
        builder.url(httpUrlBuild.build());
        return execute(builder);
    }

    /**
     * POST 请求
     *
     * @param url 请求url
     */
    public static String postJson(String url) {
        return postJson(url, "");
    }


    /**
     * POST 请求
     */
    public static String postJson(String url, String data) {
        return doParseResponseBody(postResponseBodyJson(url, data));
    }

    /**
     * POST 请求
     */
    public static String postJson(String url, Map<String, String> requestParams) {
        return doParseResponseBody(postResponseBodyJson(url, "", null, requestParams));
    }

    /**
     * POST 请求
     */
    public static String postJson(String url, String data, Map<String, String> headersParams) {
        return doParseResponseBody(postResponseBodyJson(url, data, headersParams));
    }


    public static String postJsonLong(String url, String data, Map<String, String> headersParams) {
        return doParseResponseBody(postResponseBodyLong(url, data, headersParams, JSON));
    }

    /**
     * POST 请求
     */
    public static String postJson(String url, String data, Map<String, String> headersParams, Map<String, String> requestParams) {
        return doParseResponseBody(postResponseBodyJson(url, data, headersParams, requestParams));
    }

    /**
     * POST 请求
     */
    public static String postJson(OkHttpClient client, String url, String data, Map<String, String> headersParams, Map<String, String> requestParams) {
        return doParseResponseBody(postResponseBodyJson(client, url, data, headersParams, requestParams));
    }

    /**
     * POST 请求
     */
    public static String postForm(String url) {
        return doParseResponseBody(postResponseBodyForm(url));
    }

    /**
     * POST 请求
     */
    public static String postForm(String url, String data) {
        return doParseResponseBody(postResponseBodyForm(url, data));
    }

    /**
     * POST 请求
     */
    public static String postForm(String url, Map<String, String> requestParams) {
        return doParseResponseBody(postResponseBodyForm(url, "", null, requestParams));
    }

    /**
     * POST 请求
     */
    public static String postForm(String url, String data, Map<String, String> headersParams, Map<String, String> requestParams) {
        return doParseResponseBody(postResponseBodyForm(url, data, headersParams, requestParams));
    }

    /**
     * POST 请求
     */
    public static String postForm(String url, String data, Map<String, String> headersParams) {
        return doParseResponseBody(postResponseBodyForm(url, data, headersParams, null));
    }

    /**
     * POST 请求
     */
    private static ResponseBody postResponseBodyJson(String url, String data) {
        return postResponseBody(url, data, null, JSON);
    }

    /**
     * POST 请求
     */
    private static ResponseBody postResponseBodyJson(String url, String data, Map<String, String> headersParams) {
        return postResponseBody(url, data, headersParams, JSON);
    }

    /**
     * POST 请求
     */
    private static ResponseBody postResponseBodyJson(String url, String data, Map<String, String> headersParams, Map<String, String> requestParams) {
        return postResponseBody(url, data, headersParams, requestParams, JSON);
    }

    public static ResponseBody postResponseBodyJson(OkHttpClient client, String url, String data, Map<String, String> headersParams, Map<String, String> requestParams) {
        return postResponseBody(client, url, data, headersParams, requestParams, JSON);
    }

    /**
     * POST 请求
     */
    private static ResponseBody postResponseBodyForm(String url) {
        return postResponseBody(url, "", null, JSON);
    }

    /**
     * POST 请求
     */
    private static ResponseBody postResponseBodyForm(String url, String data) {
        return postResponseBody(url, data, null, JSON);
    }

    /**
     * POST 请求
     */
    private static ResponseBody postResponseBodyForm(String url, String data, Map<String, String> headersParams) {
        return postResponseBody(url, data, headersParams, JSON);
    }

    /**
     * POST 请求
     */
    private static ResponseBody postResponseBodyForm(String url, String data, Map<String, String> headersParams, Map<String, String> requestParams) {
        return postResponseBody(url, data, headersParams, requestParams, JSON);
    }

    public static ResponseBody postOptionalResponseBodyForm(String url, String data, Map<String, String> headersParams, Map<String, String> requestParams) {

        return postResponseBody(url, data, headersParams, requestParams, JSON);
    }

    /**
     * POST 请求
     */
    public static ResponseBody postResponseBody(String url, String data, Map<String, String> headersParams, MediaType mediaType) {
        HttpUrl.Builder httpUrlBuild = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        Request.Builder builder = new Request.Builder();

        if (ObjectUtils.isNotEmpty(headersParams)) {
            builder.headers(setHeaders(headersParams));
        }
        builder.post(RequestBody.create(mediaType, data));
        builder.url(httpUrlBuild.build());
        return execute(builder);
    }


    private static ResponseBody postResponseBodyLong(String url, String data, Map<String, String> headersParams, MediaType mediaType) {
        HttpUrl.Builder httpUrlBuild = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        Request.Builder builder = new Request.Builder();
        if (ObjectUtils.isNotEmpty(headersParams)) {
            builder.headers(setHeaders(headersParams));
        }
        builder.post(RequestBody.create(mediaType, data));
        builder.url(httpUrlBuild.build());
        return executeLong(builder);
    }

    /**
     * POST 请求
     */
    private static ResponseBody postResponseBody(String url, String data, Map<String, String> headersParams, Map<String, String> requestParams, MediaType mediaType) {
        return postResponseBody(okHttpClient, url, data, headersParams, requestParams, mediaType);
    }

    public static ResponseBody postResponseBody(OkHttpClient client, String url, String data, Map<String, String> headersParams, Map<String, String> requestParams, MediaType mediaType) {
        HttpUrl.Builder httpUrlBuild = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        Optional.ofNullable(requestParams).orElse(Collections.emptyMap()).forEach(httpUrlBuild::addQueryParameter);
        Request.Builder builder = new Request.Builder();
        if (ObjectUtils.isNotEmpty(headersParams)) {
            builder.headers(setHeaders(headersParams));
        }
        builder.post(RequestBody.create(mediaType, data));
        builder.url(httpUrlBuild.build());
        return execute(client, builder);
    }


    /**
     * 设置请求头
     */
    public static Headers setHeaders(Map<String, String> headersParams) {
        Headers.Builder headersBuilder = new okhttp3.Headers.Builder();
        if (headersParams != null) {
            Iterator<String> iterator = headersParams.keySet().iterator();
            while (iterator.hasNext()) {
                final String next = iterator.next();
                headersBuilder.add(next, headersParams.get(next));
            }
        }
        return headersBuilder.build();
    }

    /**
     * put方法
     */
    public static String putJson(String url, String data, Map<String, String> headersParams) {
        return doParseResponseBody(putResponseBodyJson(url, data, headersParams));
    }

    public static String putJson(String url, String data, Map<String, String> headersParams, Map<String, String> requestParams) {
        return doParseResponseBody(putResponseBodyJson(url, data, headersParams, requestParams));
    }

    private static ResponseBody putResponseBodyJson(String url, String data, Map<String, String> headersParams) {
        HttpUrl.Builder httpUrlBuild = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        Request.Builder builder = new Request.Builder();
        if (ObjectUtils.isNotEmpty(headersParams)) {
            builder.headers(setHeaders(headersParams));
        }
        builder.put(RequestBody.create(JSON, data));
        builder.url(httpUrlBuild.build());
        return execute(builder);
    }

    private static ResponseBody putResponseBodyJson(String url, String data, Map<String, String> headersParams, Map<String, String> requestParams) {
        return putResponseBody(url, data, headersParams, requestParams, JSON);
    }

    private static ResponseBody putResponseBody(String url, String data, Map<String, String> headersParams, Map<String, String> requestParams, MediaType mediaType) {
        HttpUrl.Builder httpUrlBuild = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        Optional.ofNullable(requestParams).orElse(Collections.emptyMap()).forEach(httpUrlBuild::addQueryParameter);
        Request.Builder builder = new Request.Builder();
        if (ObjectUtils.isNotEmpty(headersParams)) {
            builder.headers(setHeaders(headersParams));
        }
        builder.put(RequestBody.create(mediaType, data));
        builder.url(httpUrlBuild.build());
        return execute(okHttpClient, builder);
    }

    /**
     * 重试机制
     *
     * @param httpRequest     ->HttpUtils里的请求方法
     * @param predicate       ->重试规则：判断httpRequest的请求结果是否符合重试规则，符合就重试,报错也重试
     * @param retryNum        ->重试次数，默认是3
     * @param sleepSecondTime ->重试等待时间，默认是1秒
     */
    public static <T> T retry(Supplier<T> httpRequest, Predicate<T> predicate, Integer retryNum, Integer sleepSecondTime) {
        T result = null;
        retryNum = ObjectUtils.isEmpty(retryNum) ? 3 : retryNum;
        sleepSecondTime = ObjectUtils.isEmpty(sleepSecondTime) ? 1 : sleepSecondTime;
        for (int i = 0; i < retryNum; i++) {
            try {
                result = httpRequest.get();
//                if (DataUtils.isEmpty(result)) return result;
                if (ObjectUtils.isEmpty(predicate)) return result;
                if (!predicate.test(result)) break;
            } catch (Exception e) {
                e.printStackTrace();
            }
            log.info("HttpUtil重试第 {} 次", i + 1);

            try {
                TimeUnit.SECONDS.sleep(sleepSecondTime);
            } catch (InterruptedException interruptedException) {
                log.info("重试报错");
            }
        }
        return result;
    }

    /**
     * 不传重试次数，就默认重试3次
     */
    public static <T> T retry(Supplier<T> httpRequest, Predicate<T> predicate) {
        return retry(httpRequest, predicate, null, null);
    }

    /**
     * 不传重试规则，就默认报错才重试
     */
    public static <T> T retry(Supplier<T> httpRequest) {
        return retry(httpRequest, null, null, null);
    }

}

