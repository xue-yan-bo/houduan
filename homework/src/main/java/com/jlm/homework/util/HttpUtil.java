package com.jlm.homework.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class HttpUtil {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

    /**
     * 发送POST请求
     * @param url 请求URL
     * @param requestBody 请求体
     * @param headers 请求头
     * @return 响应体
     * @throws IOException IO异常
     * @throws InterruptedException 中断异常
     */
    public static String sendPostRequest(String url, String requestBody, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        // 添加请求头
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(requestBuilder::header);
        }

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    /**
     * 发送POST请求（默认Content-Type为application/json）
     * @param url 请求URL
     * @param requestBody 请求体
     * @return 响应体
     * @throws IOException IO异常
     * @throws InterruptedException 中断异常
     */
    public static String sendPostRequest(String url, String requestBody) throws IOException, InterruptedException {
        return sendPostRequest(url, requestBody, Map.of("Content-Type", "application/json"));
    }

    /**
     * 发送GET请求
     * @param url 请求URL
     * @param headers 请求头
     * @return 响应体
     * @throws IOException IO异常
     * @throws InterruptedException 中断异常
     */
    public static String sendGetRequest(String url, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET();

        // 添加请求头
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(requestBuilder::header);
        }

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    /**
     * 发送GET请求（无请求头）
     * @param url 请求URL
     * @return 响应体
     * @throws IOException IO异常
     * @throws InterruptedException 中断异常
     */
    public static String sendGetRequest(String url) throws IOException, InterruptedException {
        return sendGetRequest(url, null);
    }
}
