package com.realkinetic.app.gabby.model.dto;

import org.hibernate.validator.constraints.NotEmpty;

public class ClientMessage {
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @NotEmpty
    private String message;

    public ClientMessage() {}

    public ClientMessage(final String message) {
        this.message = message;
    }
}
