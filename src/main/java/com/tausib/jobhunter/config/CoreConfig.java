package com.tausib.jobhunter.config;

import com.tausib.jobhunter.apify.ApifyClient;
import com.tausib.jobhunter.core.JobFilter;
import com.tausib.jobhunter.core.MatchScorer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Core classes are plain Java (easy to unit-test); this class turns them into Spring beans.
 */
@Configuration
public class CoreConfig {

    @Bean
    JobFilter jobFilter(JobHunterProperties props) {
        return new JobFilter(props.discovery().maxYearsExperience(), props.discovery().locationMustContain());
    }

    @Bean
    MatchScorer matchScorer() {
        return new MatchScorer(MatchScorer.defaultProfile());
    }

    @Bean
    ApifyClient apifyClient(JobHunterProperties props) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(props.apify().timeout());

        RestClient restClient = RestClient.builder()
                .baseUrl(props.apify().baseUrl())
                .requestFactory(factory)
                // token goes in a header, not the URL, so it never lands in logs
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.apify().token())
                .build();
        return new ApifyClient(restClient);
    }
}
