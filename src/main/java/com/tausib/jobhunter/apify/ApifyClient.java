package com.tausib.jobhunter.apify;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Thin wrapper over Apify's "run actor synchronously and return dataset items" endpoint:
 *   POST https://api.apify.com/v2/acts/{actorId}/run-sync-get-dataset-items
 *
 * One HTTP call = start the actor, wait for it to finish, get the scraped items back.
 * Actor IDs like "harvestapi/linkedin-job-search" are sent as "harvestapi~linkedin-job-search".
 */
public class ApifyClient {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> ITEMS =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public ApifyClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<Map<String, Object>> runSync(String actorId, String inputJson) {
        List<Map<String, Object>> items = restClient.post()
                .uri("/v2/acts/{actor}/run-sync-get-dataset-items", actorId.replace('/', '~'))
                .contentType(MediaType.APPLICATION_JSON)
                .body(inputJson)
                .retrieve()
                .body(ITEMS);
        return items == null ? List.of() : items;
    }
}
