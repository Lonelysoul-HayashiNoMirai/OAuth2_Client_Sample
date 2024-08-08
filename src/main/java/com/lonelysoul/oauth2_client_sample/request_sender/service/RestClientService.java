package com.lonelysoul.oauth2_client_sample.request_sender.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;

import static org.springframework.web.util.UriComponentsBuilder.newInstance;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestClientService {

    private final RestClient restClient;

    @Async
    public void fireATestRequest (){
        URI uri = newInstance ()
                .path ("/api/v1/protected-string")
                .build ()
                .toUri ();

        String responseBody =  restClient.get ()
                .uri (uri)
                .retrieve ()
                .onStatus (HttpStatusCode::isError, (request, response) -> {
                    log.error ("Http error status code: {}", response.getStatusCode ().value ());
                })
                .body (String.class);

        log.info ("The response: {}", responseBody);
    }
}
