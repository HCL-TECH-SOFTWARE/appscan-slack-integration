package com.hcl.appscan.slackapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
// This class represents the application details returned by the AppScan API.
// It contains various properties related to the application, including risk ratings, issue counts, and metadata.
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppScanApp {
    @JsonProperty("Id")
    private String id;
    @JsonProperty("Name")
    private String name;
    @JsonProperty("RiskRating")
    private String riskRating;
    @JsonProperty("TotalIssues")
    private int totalIssues;
    @JsonProperty("CriticalIssues")
    private int criticalIssues;
    @JsonProperty("HighIssues")
    private int highIssues;
    @JsonProperty("MediumIssues")
    private int mediumIssues;
    @JsonProperty("LowIssues")
    private int lowIssues;
    @JsonProperty("LastScanExecution")
    private Map<String, Object> lastScanExecution;
    @JsonProperty("BusinessImpact")
    private String businessImpact;
    @JsonProperty("CreatedBy")
    private String createdBy;
    @JsonProperty("DateCreated")
    private String dateCreated;
    @JsonProperty("NewIssues")
    private int newIssues;
    @JsonProperty("OpenIssues")
    private int openIssues;
    @JsonProperty("IssuesInProgress")
    private int issuesInProgress;
    @JsonProperty("OverallCompliance")
    private String overallCompliance;
    @JsonProperty("TestingStatus")
    private String testingStatus;


    public String getId() {
        return id != null ? id : "";
    }

    public String getName() {
        return name != null ? name : "Unknown";
    }

    public String getRiskRating() {
        return riskRating != null ? riskRating : "N/A";
    }

    public int getTotalIssues() {
        return totalIssues;
    }

    public int getCriticalIssues() {
        return criticalIssues;
    }

    public int getHighIssues() {
        return highIssues;
    }

    public int getMediumIssues() {
        return mediumIssues;
    }

    public int getLowIssues() {
        return lowIssues;
    }

    public String getBusinessImpact() {
        return businessImpact;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public int getNewIssues() {
        return newIssues;
    }

    public int getOpenIssues() {
        return openIssues;
    }

    public int getIssuesInProgress() {
        return issuesInProgress;
    }

    public String getOverallCompliance() {
        return overallCompliance;
    }

    public String getTestingStatus() {
        return testingStatus;
    }
}
