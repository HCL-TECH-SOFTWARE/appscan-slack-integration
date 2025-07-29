package com.hcl.appscan.slackapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
// This class represents the response from the API when logging in with an API key.
// It contains the token and its expiration time.
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiKeyLoginResponse {

    @JsonProperty("Token")
    private String token;
    @JsonProperty("Expire")
    private Instant expire;

    public String getToken() {
        return token;
    }

    public Instant getExpire() {
        return expire;
    }
}
