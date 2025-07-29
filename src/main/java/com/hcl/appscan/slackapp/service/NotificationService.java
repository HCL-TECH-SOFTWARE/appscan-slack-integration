package com.hcl.appscan.slackapp.service;

import com.hcl.appscan.slackapp.config.ChannelAppMappingConfig;
import com.hcl.appscan.slackapp.config.UserAppMappingConfig;
import com.hcl.appscan.slackapp.model.CreatedBy;
import com.hcl.appscan.slackapp.model.FullScanDetails;
import com.hcl.appscan.slackapp.model.LatestExecution;
import com.slack.api.bolt.App;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.block.ActionsBlock;
import com.slack.api.model.block.HeaderBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.composition.TextObject;
import com.slack.api.model.block.element.ButtonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
/**
 * Service component responsible for sending scan completion notifications to Slack channels.
 * <p>
 * Builds Slack message blocks with scan details and posts them to the configured notification channel
 * using the Slack API. Handles error logging and message formatting for completed scans.
 * </p>
 */
@Component
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final App slackApp;
    //private final String notificationChannel;
    private final ChannelAppMappingConfig channelAppMappingConfig;
    private final UserAppMappingConfig userAppMappingConfig;
    @Value("${appscan.api.baseurl}")
    private String appScanBaseUrl;
    public NotificationService(@Lazy App slackApp, ChannelAppMappingConfig channelAppMappingConfig, UserAppMappingConfig userAppMappingConfig) {
        this.slackApp = slackApp;
        //this.notificationChannel = notificationChannel;

        this.channelAppMappingConfig = channelAppMappingConfig;
        this.userAppMappingConfig = userAppMappingConfig;
    }

    public void sendScanCompletionNotification(FullScanDetails scanDetails) {
        LatestExecution execution = scanDetails.getLatestExecution();
        if (execution == null) {
            logger.warn("Scan {} has no 'LatestExecution' data. Cannot send notification.", scanDetails.getId());
            return;
        }
        String appName = scanDetails.getAppName();
        List<String> channels = channelAppMappingConfig.getChannelsForApp(appName);
        if (channels.isEmpty()) {
            logger.warn("No Slack channels mapped for AppScan app: {}", appName);
            return;
        }
        List<LayoutBlock> blocks = buildScanCompletionBlocks(scanDetails);

        for (String channel : channels) {
            try {
                ChatPostMessageResponse response = slackApp.client().chatPostMessage(r -> r.channel(channel).blocks(blocks).text("Scan completed for " + appName));
                if (response.isOk()) {
                    logger.info("Successfully sent scan completion notification for scan {} to channel {}", scanDetails.getId(), channel);
                } else {
                    logger.error("Failed to send Slack notification. Slack API responded with an error: {}", response.getError());
                }
            } catch (IOException | SlackApiException e) {
                logger.error("Exception while sending Slack notification for scan {}: {}", scanDetails.getId(), e.getMessage(), e);
            }
        }

        // Notify mapped users
        List<String> userIds = userAppMappingConfig.getUsersForApp(appName);
        for (String userId : userIds) {
            // Send direct message to user

            try {
                ChatPostMessageResponse response = slackApp.client().chatPostMessage(r -> r.channel(userId).blocks(blocks).text("Scan completed for " + appName));
                if (response.isOk()) {
                    logger.info("Successfully sent scan completion notification for scan {} to user {}", scanDetails.getId(), userId);
                } else {
                    logger.error("Failed to send Slack notification. Slack API responded with an error: {}", response.getError());
                }
            } catch (IOException | SlackApiException e) {
                logger.error("Exception while sending Slack notification for scan {}: {}", scanDetails.getId(), e.getMessage(), e);
            }
        }

    }

    public  List<LayoutBlock> buildScanCompletionBlocks(FullScanDetails scanDetails) {
        LatestExecution execution = scanDetails.getLatestExecution();
        if (execution == null) {
            return List.of(SectionBlock.builder().text(MarkdownTextObject.builder().text("Scan " + scanDetails.getId() + " has no 'LatestExecution' data.").build()).build());
        }

        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(HeaderBlock.builder().text(PlainTextObject.builder().text("✅ Scan " +scanDetails.getLatestExecution().getStatus()+" : " + scanDetails.getName()).emoji(true).build()).build());

        List<TextObject> fields = new ArrayList<>();
        fields.add(MarkdownTextObject.builder().text("*Application:*\n" + scanDetails.getAppName()).build());
        fields.add(MarkdownTextObject.builder().text("*Scan Name:*\n" + scanDetails.getName()).build());
        fields.add(MarkdownTextObject.builder().text("*Technology:*\n" + scanDetails.getTechnology()).build());

        CreatedBy createdBy = scanDetails.getCreatedBy();
        if (createdBy != null) {
            fields.add(MarkdownTextObject.builder().text("*Created By:*\n" + createdBy.getFullName()).build());
            fields.add(MarkdownTextObject.builder().text("*User Name:*\n" + createdBy.getUserName()).build());
            fields.add(MarkdownTextObject.builder().text("*Email:*\n" + createdBy.getEmail()).build());
        }

        if (scanDetails.getCreatedAt() != null && scanDetails.getCreatedAt().length() >= 10) {
            fields.add(MarkdownTextObject.builder().text("*Created At:*\n" + scanDetails.getCreatedAt().substring(0, 10)).build());
        }

        blocks.add(SectionBlock.builder().fields(fields).build());

        String issueBreakdown = String.format(":bell: *Total:* %d \n :black_circle: *Critical:* %d | :red_circle: *High:* %d | :large_orange_circle: *Medium:* %d | :large_blue_circle: *Low:* %d | :white_circle: *Info:* %d",
                execution.getNIssuesFound(),execution.getNCriticalIssues(), execution.getNHighIssues(), execution.getNMediumIssues(), execution.getNLowIssues(), execution.getNInfoIssues()
        );
        blocks.add(SectionBlock.builder().text(MarkdownTextObject.builder().text("*Issue Summary:*\n" + issueBreakdown).build()).build());
        blocks.add(ActionsBlock.builder().elements(List.of(
                ButtonElement.builder().text(PlainTextObject.builder().text("View in AppScan").emoji(true).build()).url(appScanBaseUrl+"/main/scans/" + scanDetails.getId()).actionId("view_scan_report_button").build(),
                ButtonElement.builder()
                        .text(PlainTextObject.builder().text("Generate Report").emoji(true).build())
                        .actionId("download_report_button")
                        .value(scanDetails.getId()) // Store scanId here
                        .build()
        )).build());

        return blocks;
    }


    public void handleDownloadReportButton(String channelId, String userId, String scanId, AppScanService appScanService) {
        // 1. Immediately acknowledge the request
        try {
            slackApp.client().chatPostMessage(r -> r
                    .channel(channelId)
                    .text("Generating your AppScan PDF report, please wait... :hourglass_flowing_sand:")
            );
        } catch (Exception e) {
            logger.warn("Failed to send initial acknowledgement to Slack: {}", e.getMessage());
        }

        // 2. Generate the report and send the download link
        try {
            String downloadLink = appScanService.getScanReportDownloadLink(scanId);
            // Only use the blocks parameter for the Markdown link
            slackApp.client().chatPostMessage(r -> r
                    .channel(channelId)
                    .blocks(List.of(
                            SectionBlock.builder()
                                    .text(MarkdownTextObject.builder()
                                            .text("*Your AppScan PDF report is ready.*\n<" + downloadLink + "|Download Report>")
                                            .build())
                                    .build()
                    ))
                    .text("Your AppScan PDF report is ready.") // fallback for notifications, not shown in channel
            );
        } catch (Exception e) {
            logger.error("Failed to generate report link for scan {}: {}", scanId, e.getMessage(), e);
            try {
                slackApp.client().chatPostMessage(r -> r
                        .channel(channelId)
                        .text("Failed to generate the report download link for scan " + scanId + ". Reason: " + e.getMessage())
                );
            } catch (Exception ex) {
                logger.error("Failed to send error message to Slack: {}", ex.getMessage(), ex);
            }
        }
    }
}
