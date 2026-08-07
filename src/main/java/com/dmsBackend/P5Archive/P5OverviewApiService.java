package com.dmsBackend.P5Archive;

import com.dmsBackend.entity.RetentionPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class P5OverviewApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final P5ApiTransactionLogger txLogger;

    @Value("${p5.username}") private String username;
    @Value("${p5.password}") private String password;
    @Value("${p5.client}") private String client;
    @Value("${p5.server.host}") private String serverHost;

    public P5ArchiveOverviewResponse fetchOverview(RetentionPolicy policy) {

        String url = serverHost + "/archive/overview";

        P5ApiTransactions tx = txLogger.create(
                HttpMethod.GET.name(),
                url,
                policy,
                "ARCHIVE_OVERVIEW"
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(username, password);
            headers.add("client", client);

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            String.class
                    );

            tx.setHttpStatus(response.getStatusCodeValue());
            tx.setResponseBody(response.getBody());
            txLogger.update(tx);

            return mapper.readValue(
                    response.getBody(),
                    P5ArchiveOverviewResponse.class
            );

        } catch (Exception e) {

            tx.setHttpStatus(500);
            tx.setResponseBody(e.getMessage());
            txLogger.update(tx);

            throw new RuntimeException(
                    "Failed to fetch P5 archive overview", e
            );
        }
    }
}
