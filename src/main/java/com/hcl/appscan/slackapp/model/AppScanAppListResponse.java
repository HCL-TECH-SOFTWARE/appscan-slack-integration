package com.hcl.appscan.slackapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// Wrapper class to handle the API response structure for a list of apps
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppScanAppListResponse {
    @JsonProperty("Items")
    private List<AppScanApp> items;

    public List<AppScanApp> getItems() {
        return items;
    }

    public void setItems(List<AppScanApp> items) {
        this.items = items;
    }
}
