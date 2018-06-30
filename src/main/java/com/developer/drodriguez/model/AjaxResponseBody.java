package com.developer.drodriguez.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Value;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AjaxResponseBody {

    private String message;
    private Object result;

    public AjaxResponseBody(String message) {
        this.message = message;
        this.result = null;
    }

}
