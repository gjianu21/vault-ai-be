package com.vaultai.be.agent;

import java.time.Duration;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.vaultai.be.config.AppProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Calls the internal agent over HTTP. The request and response bodies are AgentRequest/AgentResponse.
 * If the internal agent uses another format, change only the mapping in send().
 */
@Slf4j
@Component
@Profile("!mock")
public class RestAgentClient implements AgentClient {

    private final RestClient restClient;
    private final String chatPath;

    public RestAgentClient(AppProperties properties) {
        AppProperties.Agent agent = properties.agent();
        if (!StringUtils.hasText(agent.baseUrl())) {
            throw new IllegalStateException(
                    "app.agent.base-url is not set. Configure the agent endpoint or start with the 'mock' profile.");
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(agent.timeout());

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(agent.baseUrl())
                .requestFactory(requestFactory);
        if (StringUtils.hasText(agent.apiToken())) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + agent.apiToken());
        }

        this.restClient = builder.build();
        this.chatPath = StringUtils.hasText(agent.chatPath()) ? agent.chatPath() : "/chat";
    }

    @Override
    public AgentResponse send(AgentRequest request) {
        try {
            AgentResponse response = restClient.post()
                    .uri(chatPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AgentResponse.class);

            if (response == null || !StringUtils.hasText(response.reply())) {
                throw new AgentUnavailableException("The assistant returned an empty response");
            }
            return response;
        } catch (RestClientException ex) {
            log.warn("Agent call failed: {}", ex.getMessage());
            throw new AgentUnavailableException("The assistant is not available right now", ex);
        }
    }
}