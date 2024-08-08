package com.lonelysoul.oauth2_client_sample.request_sender.controller;

import com.lonelysoul.oauth2_client_sample.request_sender.service.RestClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
@RequestMapping("${apiPrefix}/v1/")
@RequiredArgsConstructor
public class TestController {

    private final RestClientService restClientService;

    @PostMapping("/rest-client:fireATestRequest")
    public ResponseEntity<String> fireATestRequest (){
        restClientService.fireATestRequest ();

        return ok ("OK");
    }
}
