package com.drodriguln.response;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@Slf4j
@RestControllerAdvice
public class MaestroExceptionHandler {

    @Autowired private MaestroResponseManager maestroResponseManager;

    @ExceptionHandler({ Exception.class })
    public ResponseEntity<MaestroResponseBody> handleAll(Exception e, WebRequest request) {
        log.error("Controller Error: [{}] at [{}].", e.toString(), request.getDescription(false));
        return maestroResponseManager.createExceptionResponse(e.toString());
    }
}
