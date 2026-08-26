package com.proofcart.catalog.openfoodfacts;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
public class OpenFoodFactsClient {

    private final RestTemplate restTemplate;

    public OpenFoodFactsClient(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    public List<OpenFoodFactsResponse.ProductData> searchProducts(String query) {
        String url = "https://world.openfoodfacts.org/cgi/search.pl?search_terms=" + query + "&search_simple=1&action=process&json=1&page_size=20";

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "ProofCart - Java - Version 1.0 - https://proofcart.com");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<OpenFoodFactsResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    OpenFoodFactsResponse.class
            );

            if (response.getBody() != null && response.getBody().getProducts() != null) {
                return response.getBody().getProducts();
            }
        } catch (Exception e) {
            System.err.println("[OpenFoodFacts] Search failed for query '" + query + "': " + e.getMessage());
        }
        
        return Collections.emptyList();
    }
}
