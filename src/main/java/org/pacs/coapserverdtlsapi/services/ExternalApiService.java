package org.pacs.coapserverdtlsapi.services;

import org.pacs.coapserverdtlsapi.models.AccessResponseModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ExternalApiService {
    private final WebClient webClient;

    @Autowired
    public ExternalApiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("http://localhost:8086/access-control")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public AccessResponseModel sendAccessRequest(String endpoint, String requestPayload) {
        return webClient.put()
                .uri("/request/{endpoint}", endpoint)
                .bodyValue(requestPayload)
                .retrieve()
                .bodyToMono(AccessResponseModel.class)
                .block();
    }
}
