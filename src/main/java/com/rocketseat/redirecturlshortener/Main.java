package com.rocketseat.redirecturlshortener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Main implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final S3Client s3 = S3Client.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input,
            Context context) {

        String pathParameter = (String) input.get("rawPath");
        String shortUrl = pathParameter.replace("/", "");
        if (shortUrl == null && shortUrl.isEmpty()) {
            throw new IllegalArgumentException("Invalid input, shortUrl: " + shortUrl);
        }

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket("rafaelor20-rocketseat-curso-java-gratuito")
                .key(shortUrl + ".json")
                .build();

        InputStream s3ObjecStream;

        try {
            s3ObjecStream = s3.getObject(request);
        } catch (Exception e) {
            throw new RuntimeException("Error getting object from S3 " + e.getMessage(), e);
        }

        UrlData urlData;

        try {
            urlData = objectMapper.readValue(s3ObjecStream, UrlData.class);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing object from S3 " + e.getMessage(), e);
        }

        long currentTime = System.currentTimeMillis() / 1000;
        if (urlData.getExpirationTime() > currentTime) {
            Map<String, Object> response = new HashMap<>();
            response.put("statusCode", "302");
            Map<String, String> headers = new HashMap<>();
            headers.put("Location", urlData.getOriginalUrl());
            response.put("headers", headers);

            return response;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", "410");
        response.put("body", "Url expired");

        return response;
    }
}