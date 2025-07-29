package com.hcl.appscan.slackapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents the detailed information of a full scan.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FullScanDetails {
    @JsonProperty("Id")
    private String id;
    @JsonProperty("Name")
    private String name;
    @JsonProperty("AppName")
    private String appName;
    @JsonProperty("Technology")
    private String technology;
    @JsonProperty("CreatedAt")
    private String createdAt;
    @JsonProperty("CreatedBy")
    private CreatedBy createdBy;
    @JsonProperty("LatestExecution")
    private LatestExecution latestExecution;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAppName() {
        return appName;
    }

    public String getTechnology() {
        return technology;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public CreatedBy getCreatedBy() {
        return createdBy;
    }

    public LatestExecution getLatestExecution() {
        return latestExecution;
    }
}
