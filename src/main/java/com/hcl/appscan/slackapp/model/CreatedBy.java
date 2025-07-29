package com.hcl.appscan.slackapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
/**
 * Model class representing the creator information for an entity in the AppScan Slack App.
 * <p>
 * Maps JSON properties such as first name, last name, username, and email.
 * Used for deserializing creator details from API responses.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatedBy {
    @JsonProperty("FirstName")
    private String firstName;
    @JsonProperty("LastName")
    private String lastName;
    @JsonProperty("UserName")
    private String userName;
    @JsonProperty("Email")
    private String email;

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getUserName() {
        return userName;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
}
