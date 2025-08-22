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

package com.hcl.appscan.slackapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcl.appscan.slackapp.model.*;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

// This service handles communication with the AppScan API, including authentication and fetching application details.
// --- AppScan API Service ---
@Component
public class AppScanService {
    private static final Logger logger = LoggerFactory.getLogger(AppScanService.class);
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String apiSecret;
    private final String apiBaseUrl;
    private final AtomicReference<ApiKeyLoginResponse> currentToken = new AtomicReference<>();

    public AppScanService(
            @Value("${appscan.api.key}") String apiKey,
            @Value("${appscan.api.secret}") String apiSecret,
            @Value("${appscan.api.baseurl}") String apiBaseUrl,
            @Value("${appscan.allowUntrusted:false}") boolean allowUntrusted,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.apiBaseUrl = apiBaseUrl + "/api/v4"; // Append the API version path
        this.objectMapper = objectMapper;
        this.httpClient = createHttpClient(allowUntrusted);
    }

    private OkHttpClient createHttpClient(boolean allowUntrusted) {
        if (allowUntrusted) {
            logger.warn("!!! SSL/TLS certificate verification is disabled. This is for testing only and is insecure. !!!");
            try {
                final TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[]{};
                            }
                        }
                };

                final SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

                return new OkHttpClient.Builder()
                        .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                        .hostnameVerifier((hostname, session) -> true)
                        .build();
            } catch (Exception e) {
                logger.error("Error creating untrusted SSL context. Falling back to default client.", e);
                return new OkHttpClient();
            }
        } else {
            return new OkHttpClient();
        }
    }

    private CompletableFuture<String> getAuthToken() {
        ApiKeyLoginResponse token = currentToken.get();
        if (token == null || token.getExpire().isBefore(Instant.now().plusSeconds(60))) {
            logger.info("Auth token is invalid or expiring soon. Refreshing...");
            Map<String, String> loginPayload = Map.of("KeyId", apiKey, "KeySecret", apiSecret);
            try {
                String jsonPayload = objectMapper.writeValueAsString(loginPayload);
                okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonPayload, MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder().url(apiBaseUrl + "/Account/ApiKeyLogin").post(body).build();
                return CompletableFuture.supplyAsync(() -> {
                    try (Response response = httpClient.newCall(request).execute()) {
                        if (!response.isSuccessful() || response.body() == null)
                            throw new IOException("Failed to authenticate with AppScan API: " + response);
                        ApiKeyLoginResponse newResponse = objectMapper.readValue(response.body().string(), ApiKeyLoginResponse.class);
                        currentToken.set(newResponse);
                        logger.info("Successfully refreshed AppScan auth token.");
                        return newResponse.getToken();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        }
        return CompletableFuture.completedFuture(token.getToken());
    }

    public CompletableFuture<AppScanApp> getApplicationDetailsByName(String appName) {
        return getAuthToken().thenCompose(token -> {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(apiBaseUrl + "/Apps").newBuilder();
            urlBuilder.addQueryParameter("$filter", "Name eq '" + appName.replace("'", "''") + "'");
            Request request = new Request.Builder().url(urlBuilder.build()).header("Authorization", "Bearer " + token).build();
            return CompletableFuture.supplyAsync(() -> {
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null)
                        throw new IOException("Failed to fetch application by name from AppScan API: " + response);
                    AppScanAppListResponse appListResponse = objectMapper.readValue(response.body().string(), AppScanAppListResponse.class);
                    if (appListResponse == null || appListResponse.getItems() == null || appListResponse.getItems().isEmpty()) {
                        throw new RuntimeException("Application not found: " + appName);
                    }
                    return appListResponse.getItems().get(0);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    // method to return a list of first 30 applications as slack has a limit on number of characters in a message
    public CompletableFuture<List<AppScanApp>> getAllApplications() {
        return getAuthToken().thenCompose(token -> {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(apiBaseUrl + "/Apps").newBuilder();
            urlBuilder.addQueryParameter("$top", "30");
            Request request = new Request.Builder().url(urlBuilder.build()).header("Authorization", "Bearer " + token).build();
            return CompletableFuture.supplyAsync(() -> {
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null)
                        throw new IOException("Failed to fetch all applications from AppScan API: " + response);
                    AppScanAppListResponse appListResponse = objectMapper.readValue(response.body().string(), AppScanAppListResponse.class);
                    return appListResponse != null ? appListResponse.getItems() : new ArrayList<>();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    public CompletableFuture<Optional<FullScanDetails>> getLatestScanForApp(String appId) {
        return getAuthToken().thenCompose(token -> {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(apiBaseUrl + "/Scans").newBuilder();
            urlBuilder.addQueryParameter("$top", "1");
            urlBuilder.addQueryParameter("$filter", "AppId eq " + appId);
            urlBuilder.addQueryParameter("$orderby", "CreatedAt desc");
            Request request = new Request.Builder().url(urlBuilder.build()).header("Authorization", "Bearer " + token).build();
            return CompletableFuture.supplyAsync(() -> {
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null)
                        throw new IOException("Failed to fetch latest scan from AppScan API: " + response);
                    ScanListResponse scanList = objectMapper.readValue(response.body().string(), ScanListResponse.class);
                    if (scanList.getItems() == null || scanList.getItems().isEmpty()) {
                        return Optional.empty();
                    }
                    return Optional.of(scanList.getItems().get(0));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }


    public CompletableFuture<List<Map<String, String>>> getScansByUsername(String username) {
        return getAuthToken().thenCompose(token -> {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(apiBaseUrl + "/Scans").newBuilder();
            urlBuilder.addQueryParameter("$filter", "CreatedBy/UserName eq '" + username.replace("'", "''") + "'");
            urlBuilder.addQueryParameter("$top", "10");// Limit to 10 scans
            Request request = new Request.Builder()
                    .url(urlBuilder.build())
                    .header("Authorization", "Bearer " + token)
                    .build();
            return CompletableFuture.supplyAsync(() -> {
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null)
                        throw new IOException("Failed to fetch scans for user: " + username + " from AppScan API: " + response);
                    String responseBody = response.body().string();
                    // Parse only scan name and id
                    JsonNode root = objectMapper.readTree(responseBody);
                    JsonNode items = root.path("Items");
                    List<Map<String, String>> scanList = new ArrayList<>();
                    if (items.isArray()) {
                        for (JsonNode scan : items) {
                            Map<String, String> scanInfo = Map.of(
                                    "Name", scan.path("Name").asText(),
                                    "Id", scan.path("Id").asText()
                            );
                            scanList.add(scanInfo);
                        }
                    }
                    return scanList;
                } catch (IOException e) {
                    throw new RuntimeException("Failed to fetch scans for user: " + username, e);
                }
            });
        });
    }


public CompletableFuture<Optional<FullScanDetails>> getScanSummaryById(String scanId) {
    return getAuthToken().thenCompose(token -> {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(apiBaseUrl + "/Scans").newBuilder();
        urlBuilder.addQueryParameter("$filter", "Id eq " + scanId);
        urlBuilder.addQueryParameter("$top", "1");
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header("Authorization", "Bearer " + token)
                .build();
        return CompletableFuture.supplyAsync(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null)
                    throw new IOException("Failed to fetch scan summary for ID: " + scanId + " from AppScan API: " + response);
                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode items = root.path("Items");
                if (items.isArray() && items.size() > 0) {
                    // Parse as FullScanDetails
                    FullScanDetails details = objectMapper.treeToValue(items.get(0), FullScanDetails.class);
                    return Optional.of(details);
                }
                return Optional.empty();
            } catch (IOException e) {
                throw new RuntimeException("Failed to fetch scan summary for ID: " + scanId, e);
            }
        });
    });
}


    public String getScanReportDownloadLink(String scanId,String scanName) throws Exception {
        String createUrl = apiBaseUrl + "/Reports/Security/Scan/" + scanId;
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("ReportFileType", "Pdf");
        configuration.put("Summary", true);
        configuration.put("Details", true);
        configuration.put("Discussion", true);
        configuration.put("Overview", true);
        configuration.put("TableOfContent", true);
        configuration.put("History", true);
        configuration.put("Coverage", true);
        configuration.put("MinimizeDetails", true);
        configuration.put("Articles", true);
        configuration.put("Title", scanName + " - " + Instant.now().toString());
        Map<String, Object> body = Map.of("Configuration", configuration);

        RequestBody requestBody = RequestBody.create(
                objectMapper.writeValueAsString(body), MediaType.parse("application/json"));

        Request createRequest = new Request.Builder()
                .url(createUrl)
                .header("Authorization", "Bearer " + getAuthTokenSync())
                .post(requestBody)
                .build();

        String reportId;
        try (Response createResp = httpClient.newCall(createRequest).execute()) {
            if (!createResp.isSuccessful()) throw new IOException("Failed to create report: " + createResp);
            String respBody = createResp.body().string();
            reportId = objectMapper.readTree(respBody).path("Id").asText();
        }

        String statusUrl = apiBaseUrl + "/Reports?$filter=Id eq " + reportId + "&$count=false";
        String status = "";
        String downloadLink = null;
        int maxAttempts = 12;
        int attempt = 0;
        while (attempt < maxAttempts) {
            Request statusRequest = new Request.Builder()
                    .url(statusUrl)
                    .header("Authorization", "Bearer " + getAuthTokenSync())
                    .get()
                    .build();
            try (Response statusResp = httpClient.newCall(statusRequest).execute()) {
                if (!statusResp.isSuccessful()) throw new IOException("Failed to get report status: " + statusResp);
                String statusBody = statusResp.body().string();
                JsonNode items = objectMapper.readTree(statusBody).path("Items");
                if (items.isArray() && items.size() > 0) {
                    JsonNode report = items.get(0);
                    status = report.path("Status").asText();
                    if ("Ready".equalsIgnoreCase(status)) {
                        downloadLink = report.path("DownloadLink").asText();
                        break;
                    }
                }
            }
            Thread.sleep(5000);
            attempt++;
        }
        if (!"Ready".equalsIgnoreCase(status) || downloadLink == null || downloadLink.isEmpty()) {
            throw new IOException("Report not ready or download link missing after waiting.");
        }
        return downloadLink;
    }

    private String getAuthTokenSync() throws Exception {
        try {
            return getAuthToken().get();
        } catch (Exception e) {
            logger.error("Failed to get auth token synchronously", e);
            throw e;
        }
    }
}
