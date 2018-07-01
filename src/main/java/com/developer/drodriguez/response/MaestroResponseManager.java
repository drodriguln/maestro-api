package com.developer.drodriguez.response;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class MaestroResponseManager {

    private static final String FIND_SUCCESS = "Objects found.";
    private static final String FIND_FAILURE = "Could not find any objects.";
    private static final String SAVE_SUCCESS = "Object saved successfully.";
    private static final String SAVE_FAILURE = "Could not successfully save the object.";
    private static final String DELETE_SUCCESS = "Object deleted successfully.";
    private static final String DELETE_FAILURE = "Could not successfully delete the object.";

    public static ResponseEntity<MaestroResponseBody> createGetSuccessResponse(Object content) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new MaestroResponseBody(FIND_SUCCESS, content));
    }

    public static ResponseEntity<MaestroResponseBody> createGetFailureResponse() {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new MaestroResponseBody(FIND_FAILURE));
    }

    public static ResponseEntity<MaestroResponseBody> createSaveSuccessResponse(Object content) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new MaestroResponseBody(SAVE_SUCCESS, content));
    }

    public static ResponseEntity<MaestroResponseBody> createSaveFailureResponse() {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new MaestroResponseBody(SAVE_FAILURE));
    }

    public static ResponseEntity<MaestroResponseBody> createDeleteSuccessResponse() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new MaestroResponseBody(DELETE_SUCCESS));
    }

    public static ResponseEntity<MaestroResponseBody> createDeleteFailureResponse() {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new MaestroResponseBody(DELETE_FAILURE));
    }

    public static ResponseEntity<byte[]> createGetFileSuccessResponse(HttpHeaders headers, byte[] bytes) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .headers(headers)
                .body(bytes);
    }

    public static ResponseEntity<byte[]> createGetFileFailureResponse() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new byte[0]);
    }

}
