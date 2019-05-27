package com.drodriguez.maestro.api.response

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest

@RestControllerAdvice
class MaestroExceptionHandler {

    @Autowired
    private lateinit var maestroResponseManager: MaestroResponseManager

    @ExceptionHandler(Exception::class)
    fun handleAll(e: Exception, request: WebRequest): ResponseEntity<MaestroResponseBody> {
        logger.error("Controller Error: [{}] at [{}].", e.toString(), request.getDescription(false))
        return maestroResponseManager.createExceptionResponse(e.toString())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MaestroExceptionHandler::class.java)
    }
}
