package com.lonelysoul.oauth2_client_sample.request_sender.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class CatchUnhandledExceptionHandler {

    @ExceptionHandler(Exception.class)
    public void handleUnhandledException (Exception exception){
        log.error (exception.getLocalizedMessage (), exception);
    }
}
