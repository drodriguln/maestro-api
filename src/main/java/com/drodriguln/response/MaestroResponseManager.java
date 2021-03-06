package com.drodriguln.response;

import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class MaestroResponseManager {

    private static final String FIND_SUCCESS = "Objects found.";
    private static final String FIND_FAILURE = "Could not find any objects.";
    private static final String SAVE_SUCCESS = "Object saved successfully.";
    private static final String SAVE_FAILURE = "Could not successfully save the object.";
    private static final String DELETE_SUCCESS = "Object deleted successfully.";
    private static final String DELETE_FAILURE = "Could not successfully delete the object.";

    public ResponseEntity<MaestroResponseBody> createGetSuccessResponse(Object content) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new MaestroResponseBody(FIND_SUCCESS, content));
    }

    public ResponseEntity<MaestroResponseBody> createGetFailureResponse() {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new MaestroResponseBody(FIND_FAILURE));
    }

    public ResponseEntity<MaestroResponseBody> createSaveSuccessResponse(Object content) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new MaestroResponseBody(SAVE_SUCCESS, content));
    }

    public ResponseEntity<MaestroResponseBody> createSaveFailureResponse() {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new MaestroResponseBody(SAVE_FAILURE));
    }

    public ResponseEntity<MaestroResponseBody> createDeleteSuccessResponse() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new MaestroResponseBody(DELETE_SUCCESS));
    }

    public ResponseEntity<MaestroResponseBody> createDeleteFailureResponse() {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new MaestroResponseBody(DELETE_FAILURE));
    }

    public ResponseEntity<GridFsResource> createGetFileSuccessResponse(HttpHeaders headers, GridFsResource file) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .headers(headers)
                .body(file);
    }

    public ResponseEntity<GridFsResource> createGetFileFailureResponse() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    public ResponseEntity<MaestroResponseBody> createExceptionResponse(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MaestroResponseBody(message));
    }

}
