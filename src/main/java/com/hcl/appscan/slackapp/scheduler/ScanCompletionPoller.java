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

package com.hcl.appscan.slackapp.scheduler;

import com.hcl.appscan.slackapp.config.ChannelAppMappingConfig;
import com.hcl.appscan.slackapp.model.FullScanDetails;
import com.hcl.appscan.slackapp.model.LatestExecution;
import com.hcl.appscan.slackapp.service.AppScanService;
import com.hcl.appscan.slackapp.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
/**
 * Scheduled component that polls AppScan for completed scans of monitored applications.
 * <p>
 * Periodically checks the status of the latest scan for each configured application.
 * If a scan is found to be complete ("Ready" status) and has not been notified yet,
 * sends a notification using the {@link NotificationService}.
 * </p>
 * <p>
 * Uses a concurrent map to track the last notified scan ID for each application,
 * ensuring notifications are only sent once per completed scan.
 * </p>
 */

@Component
public class ScanCompletionPoller {
    private static final Logger logger = LoggerFactory.getLogger(ScanCompletionPoller.class);
    private final AppScanService appScanService;
    private final NotificationService notificationService;
    private final ChannelAppMappingConfig channelAppMappingConfig;
    private final Map<String, String> lastNotifiedScanIds = new ConcurrentHashMap<>();

    public ScanCompletionPoller(AppScanService appScanService, NotificationService notificationService, ChannelAppMappingConfig channelAppMappingConfig) {
        this.appScanService = appScanService;
        this.notificationService = notificationService;
        this.channelAppMappingConfig = channelAppMappingConfig;
    }

    @Scheduled(fixedRateString = "${appscan.poller.rate.ms}")
    public void checkForCompletedScans() {
        logger.info("Polling for completed scans for monitored applications...");
        Set<String> monitoredApps = channelAppMappingConfig.getAllAppNames();
        if (monitoredApps.isEmpty()) {
            logger.warn("No applications configured for monitoring. Skipping poll cycle.");
            return;
        }
        monitoredApps.forEach(appName -> {
            appScanService.getApplicationDetailsByName(appName)
                    .thenCompose(appDetails -> appScanService.getLatestScanForApp(appDetails.getId()))
                    .thenAccept(latestScanOpt -> {
                        if (latestScanOpt.isPresent()) {
                            FullScanDetails latestScan = latestScanOpt.get();
                            String currentScanId = latestScan.getId();
                            String lastNotifiedScanId = lastNotifiedScanIds.get(appName);
                            if (currentScanId != null && !currentScanId.equals(lastNotifiedScanId)) {
                                String status = Optional.ofNullable(latestScan.getLatestExecution()).map(LatestExecution::getStatus).orElse("Unknown");
                                logger.info("Found latest scan {} for application {}. Status: {}", currentScanId, appName, status);
                                if ("Ready".equalsIgnoreCase(status)) {
                                    logger.info("Scan {} is complete. Sending notification.", currentScanId);
                                    notificationService.sendScanCompletionNotification(latestScan);
                                    lastNotifiedScanIds.put(appName, currentScanId);
                                } else {
                                    logger.info("Scan {} is not 'Ready' yet. Will check again on the next poll cycle.", currentScanId);
                                }
                            }
                        } else {
                            logger.info("No scans found for application {}", appName);
                        }
                    }).exceptionally(ex -> {
                        logger.error("Failed to poll application: " + appName, ex);
                        return null;
                    });
        });
    }
}