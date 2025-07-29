package com.hcl.appscan.slackapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
// Wrapper class to handle the API response structure for a list of full scan details
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScanListResponse {
    @JsonProperty("Items")
    private List<FullScanDetails> items;

    public List<FullScanDetails> getItems() {
        return items;
    }

    public void setItems(List<FullScanDetails> items) {
        this.items = items;
    }
}
