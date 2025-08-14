package com.beta.apiservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class QstashPublisher {

    @Value("${qstash.url:https://qstash.upstash.io}")
    private String qstashBaseUrl;

    @Value("${qstash.token:}")
    private String qstashToken;

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl(qstashBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + qstashToken)
                .build();
    }

    public void publishJson(String targetUrl, String jsonPayload) {
        if (qstashToken == null || qstashToken.isBlank()) {
            throw new IllegalStateException("QStash token is not configured (qstash.token / QSTASH_TOKEN)");
        }
        String path = "/v2/publish/" + targetUrl;
        restClient()
                .post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonPayload)
                .retrieve()
                .toBodilessEntity();
        log.debug("Published message to QStash for target: {}", targetUrl);
    }
}


