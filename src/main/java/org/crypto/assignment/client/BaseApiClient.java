package org.crypto.assignment.client;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

public abstract class BaseApiClient {
    protected final RestTemplate restTemplate;

    protected BaseApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    protected HttpEntity<?> createRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON, MediaType.ALL));
        return new HttpEntity<>(headers);
    }

    protected HttpEntity<?> createRequestEntity(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON, MediaType.ALL));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}