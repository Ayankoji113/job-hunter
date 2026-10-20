package com.tausib.jobhunter.apify;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Verifies the HTTP call shape without touching the real Apify API (no token, no cost). */
class ApifyClientTest {

    @Test
    void callsRunSyncEndpointAndParsesItems() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.apify.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ApifyClient client = new ApifyClient(builder.build());

        server.expect(requestTo("https://api.apify.com/v2/acts/harvestapi~linkedin-job-search/run-sync-get-dataset-items"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"keyword\":\"Java Intern\"}"))
                .andRespond(withSuccess("[{\"title\":\"Java Intern\",\"jobUrl\":\"https://x.com/1\"}]",
                        MediaType.APPLICATION_JSON));

        List<Map<String, Object>> items = client.runSync("harvestapi/linkedin-job-search", "{\"keyword\":\"Java Intern\"}");

        assertEquals(1, items.size());
        assertEquals("Java Intern", items.get(0).get("title"));
        server.verify();
    }
}
