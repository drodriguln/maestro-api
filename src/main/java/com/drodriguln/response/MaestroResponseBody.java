package com.drodriguln.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MaestroResponseBody {

    private String message;
    private Object data;

    MaestroResponseBody(String message) {
        this.message = message;
        this.data = null;
    }

    MaestroResponseBody(String message, Object data) {
        this.message = message;
        this.data = data;
    }

}
