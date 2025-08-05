/*
 *
 *  *
 *  * Copyright 2025 HCL America, Inc.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  * /
 *
 */

package com.hcl.appscan.slackapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
/**
 * Represents the latest execution status of a scan.
 * Contains information about the status and number of issues categorized by severity.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LatestExecution {
    @JsonProperty("Status")
    private String status;
    @JsonProperty("NIssuesFound")
    private int nIssuesFound;
    @JsonProperty("NCriticalIssues")
    private int nCriticalIssues;
    @JsonProperty("NHighIssues")
    private int nHighIssues;
    @JsonProperty("NMediumIssues")
    private int nMediumIssues;
    @JsonProperty("NLowIssues")
    private int nLowIssues;
    @JsonProperty("NInfoIssues")
    private int nInfoIssues;

    public String getStatus() {
        return status;
    }

    public int getNCriticalIssues() {
        return nCriticalIssues;
    }

    public int getNHighIssues() {
        return nHighIssues;
    }

    public int getNMediumIssues() {
        return nMediumIssues;
    }

    public int getNLowIssues() {
        return nLowIssues;
    }

    public int getNInfoIssues() {
        return nInfoIssues;
    }

    public int getNIssuesFound() {
        return nIssuesFound;
    }
}
