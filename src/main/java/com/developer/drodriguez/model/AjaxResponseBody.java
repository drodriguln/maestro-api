package com.developer.drodriguez.model;

import java.util.List;

public class AjaxResponseBody {
    String message;
    Object result;

    public AjaxResponseBody() {}

    public AjaxResponseBody(String message) {
        this.message = message;
        this.result = null;
    }

    public AjaxResponseBody(String message, Object result) {
        this.message = message;
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
