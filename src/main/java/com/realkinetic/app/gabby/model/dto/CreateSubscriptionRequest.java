package com.realkinetic.app.gabby.model.dto;

import org.hibernate.validator.constraints.NotEmpty;

public class CreateSubscriptionRequest {
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NotEmpty
    private String name;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    private String token;
}
