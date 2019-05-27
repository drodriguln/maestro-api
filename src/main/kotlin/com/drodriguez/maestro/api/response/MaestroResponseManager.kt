package com.drodriguez.maestro.api.response

import org.springframework.data.mongodb.gridfs.GridFsResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
class MaestroResponseManager {

    fun createGetSuccessResponse(content: Any): ResponseEntity<MaestroResponseBody> {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(MaestroResponseBody(FIND_SUCCESS, content))
    }

    fun createGetFailureResponse(): ResponseEntity<MaestroResponseBody> {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(MaestroResponseBody(FIND_FAILURE))
    }

    fun createSaveSuccessResponse(content: Any): ResponseEntity<MaestroResponseBody> {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(MaestroResponseBody(SAVE_SUCCESS, content))
    }

    fun createSaveFailureResponse(): ResponseEntity<MaestroResponseBody> {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(MaestroResponseBody(SAVE_FAILURE))
    }

    fun createDeleteSuccessResponse(): ResponseEntity<MaestroResponseBody> {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(MaestroResponseBody(DELETE_SUCCESS))
    }

    fun createDeleteFailureResponse(): ResponseEntity<MaestroResponseBody> {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(MaestroResponseBody(DELETE_FAILURE))
    }

    fun createGetFileSuccessResponse(headers: HttpHeaders, file: GridFsResource): ResponseEntity<GridFsResource> {
        return ResponseEntity
                .status(HttpStatus.OK)
                .headers(headers)
                .body(file)
    }

    fun createGetFileFailureResponse(): ResponseEntity<GridFsResource> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
    }

    fun createExceptionResponse(message: String): ResponseEntity<MaestroResponseBody> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(MaestroResponseBody(message))
    }

    companion object {
        private const val FIND_SUCCESS = "Objects found."
        private const val FIND_FAILURE = "Could not find any objects."
        private const val SAVE_SUCCESS = "Object saved successfully."
        private const val SAVE_FAILURE = "Could not successfully save the object."
        private const val DELETE_SUCCESS = "Object deleted successfully."
        private const val DELETE_FAILURE = "Could not successfully delete the object."
    }

}
