package com.hcl.appscan.slackapp.config;

import com.hcl.appscan.slackapp.model.FullScanDetails;
import com.hcl.appscan.slackapp.service.AppScanService;
import com.hcl.appscan.slackapp.model.AppScanApp;
import com.hcl.appscan.slackapp.service.NotificationService;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.model.block.*;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.ButtonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Spring configuration class for setting up the Slack App integration.
 * <p>
 * Registers the `/appscan` Slack command and handles subcommands such as
 * `summary`, `list_apps`, `scan_summary`,'list_scans' and `help`. Integrates with the AppScanService
 * to fetch application data and formats responses for Slack.
 * </p>
 *
 * Dependencies:
 * <ul>
 *   <li>Spring Boot</li>
 *   <li>Slack Bolt SDK</li>
 *   <li>AppScanService</li>
 * </ul>
 *
 * Configure the Slack bot token via the `slack.bot.token` property.
 */
@Configuration
public class SlackAppConfig {
    private static final Logger logger = LoggerFactory.getLogger(SlackAppConfig.class);

    @Bean
    public App slackApp(AppScanService appScanService, NotificationService notificationService, @Value("${slack.bot.token}") String botToken , @Value("${appscan.api.baseurl}") String appScanBaseUrl) {
        AppConfig config = new AppConfig();
        config.setSingleTeamBotToken(botToken);
        App app = new App(config);

        app.blockAction("download_report_button", (req, ctx) -> {
            String channelId = req.getPayload().getChannel().getId();
            String userId = req.getPayload().getUser().getId();
            String scanId = req.getPayload().getActions().get(0).getValue();

            notificationService.handleDownloadReportButton(channelId, userId, scanId, appScanService);

            return ctx.ack();
        });


        app.command("/appscan", (req, ctx) -> {
            // After extracting commandText, subCommand, and commandValue
            String commandText = req.getPayload().getText() != null ? req.getPayload().getText().trim() : "";
            String[] args = commandText.split("\\s+", 2);
            String subCommand = args.length > 0 ? args[0].trim() : "";
            String commandValue = args.length > 1 ? args[1].trim() : "";

            // Validate input
            if (subCommand.isEmpty()) {
                return ctx.ack("Please provide a valid subcommand. Try `/appscan help`.");
            }
            logger.info("Received /appscan command: subCommand='{}', value='{}'", subCommand, commandValue);
            // Validate commandValue for specific subcommands
            if ("summary".equalsIgnoreCase(subCommand) && commandValue.isEmpty()) {
                return ctx.ack("Please provide an application name for the `summary` command. Try `/appscan summary <Application Name>`.");
            }
            if ("summary".equalsIgnoreCase(subCommand) && !commandValue.isEmpty()) {
                ctx.ack();
                appScanService.getApplicationDetailsByName(commandValue)
                        .thenAccept(summary -> {
                            logger.info("Summary fetched for application: {}", summary.getName());
                            String formattedDate = "N/A";
                            if (summary.getDateCreated() != null && summary.getDateCreated().length() >= 10) {
                                formattedDate = summary.getDateCreated().substring(0, 10);
                            }
                            List<LayoutBlock> blocks = Arrays.asList(
                                    HeaderBlock.builder().text(PlainTextObject.builder().text("Application Summary: " + summary.getName()).emoji(true).build()).build(),
                                    SectionBlock.builder().fields(Arrays.asList(
                                            MarkdownTextObject.builder().text("*Application:*\n" + summary.getName()).build(),
                                            MarkdownTextObject.builder().text("*Overall Risk:*\n*" + summary.getRiskRating() + "*").build(),
                                            MarkdownTextObject.builder().text("*Total Issues:*\n" + summary.getTotalIssues()).build(),
                                            MarkdownTextObject.builder().text("*Business Impact:*\n" + summary.getBusinessImpact()).build(),
                                            MarkdownTextObject.builder().text("*Created By:*\n" + summary.getCreatedBy()).build(),
                                            MarkdownTextObject.builder().text("*Date Created:*\n" + formattedDate).build()
                                    )).build(),
                                    SectionBlock.builder().fields(Arrays.asList(
                                            MarkdownTextObject.builder().text("*New Issues:*\n" + summary.getNewIssues()).build(),
                                            MarkdownTextObject.builder().text("*Open Issues:*\n" + summary.getOpenIssues()).build(),
                                            MarkdownTextObject.builder().text("*In-Progress Issues:*\n" + summary.getIssuesInProgress()).build(),
                                            MarkdownTextObject.builder().text("*Compliance:*\n" + summary.getOverallCompliance()).build(),
                                            MarkdownTextObject.builder().text("*Testing Status:*\n" + summary.getTestingStatus()).build()
                                    )).build(),
                                    SectionBlock.builder().fields(Arrays.asList(
                                            MarkdownTextObject.builder().text("*Critical Issues:*\n" + summary.getCriticalIssues()).build(),
                                            MarkdownTextObject.builder().text("*High Issues:*\n" + summary.getHighIssues()).build(),
                                            MarkdownTextObject.builder().text("*Medium Issues:*\n" + summary.getMediumIssues()).build(),
                                            MarkdownTextObject.builder().text("*Low Issues:*\n" + summary.getLowIssues()).build()
                                    )).build(),
                                    ActionsBlock.builder().elements(List.of(
                                            ButtonElement.builder().text(PlainTextObject.builder().text("View in AppScan").emoji(true).build()).url(appScanBaseUrl+"/main/myapps/" + summary.getId()).actionId("view_in_appscan_button").build()
                                    )).build()
                            );
                            try {
                                ctx.respond(r -> r.responseType("in_channel").blocks(blocks));
                            } catch (IOException e) {
                                logger.error("Failed to respond to summary command", e);
                            }
                        })
                        .exceptionally(ex -> {
                            logger.error("Error fetching or processing AppScan summary", ex);
                            try {
                                ctx.respond("Failed to fetch summary for `" + commandValue + "`. Reason: " + ex.getMessage());
                            } catch (IOException e) {
                                logger.error("Failed to send error response", e);
                            }
                            return null;
                        });
            } else if ("list_apps".equalsIgnoreCase(subCommand)) {
                ctx.ack();
                appScanService.getAllApplications()
                        .thenAccept(apps -> {
                            logger.info("Fetched {} applications from AppScan", apps.size());
                            List<LayoutBlock> blocks = new ArrayList<>();
                            blocks.add(HeaderBlock.builder().text(PlainTextObject.builder().text("AppScan Applications").emoji(true).build()).build());

                            for (AppScanApp appItem : apps) {
                                String appDetails = String.format("*%s*\n*Risk:* %s | *Total Issues:* %d",
                                        appItem.getName(), appItem.getRiskRating(), appItem.getTotalIssues());
                                blocks.add(SectionBlock.builder().text(MarkdownTextObject.builder().text(appDetails).build()).build());
                                blocks.add(DividerBlock.builder().build());
                            }

                            try {
                                logger.info("Attempting to respond to Slack with app list...");
                                ctx.respond(r -> r.responseType("in_channel").blocks(blocks));
                            } catch (Exception e) {
                                logger.error("Failed to respond to list_apps command", e);
                            }
                        })
                        .exceptionally(ex -> {
                            logger.error("Error fetching application list", ex);
                            try {
                                ctx.respond("Failed to fetch application list. Reason: " + ex.getMessage());
                            } catch (IOException e) {
                                logger.error("Failed to send error response", e);
                            }
                            return null;
                        });
            } else if ("help".equalsIgnoreCase(subCommand)) {
                return ctx.ack("Available commands:\n`/appscan summary <Application Name>`\n`/appscan list_apps`\n`/appscan list_scans <username>` \n `/appscan scan_summary <ID>`\n\nFor more information, visit the [AppScan Documentation](https://www.hcl-software.com/appscan/home).");
            } else if ("list_scans".equalsIgnoreCase(subCommand)) {
                if (commandValue.isEmpty()) {
                    return ctx.ack("Please provide a username. Usage: `/appscan list_scans <username>`");
                }
                ctx.ack();
                String username = commandValue;
                appScanService.getScansByUsername(username)
                        .thenAccept(scanList -> {
                            try {
                                if (scanList.isEmpty()) {
                                    ctx.respond("No scans found for user: " + username);
                                } else {
                                    List<LayoutBlock> blocks = new ArrayList<>();
                                    blocks.add(HeaderBlock.builder().text(PlainTextObject.builder().text("Your Scans").emoji(true).build()).build());
                                    for (Map<String, String> scan : scanList) {
                                        blocks.add(SectionBlock.builder()
                                                .text(MarkdownTextObject.builder()
                                                        .text("*Name:* " + scan.get("Name") + "\n*Id:* " + scan.get("Id"))
                                                        .build())
                                                .build());
                                    }
                                    ctx.respond(r -> r.responseType("in_channel").blocks(blocks));
                                }
                            } catch (Exception e) {
                                logger.error("Failed to respond with scan list for user: {}", username, e);
                                try {
                                    ctx.respond("Failed to fetch scans for user: " + username + ". Reason: " + e.getMessage());
                                } catch (IOException ioException) {
                                    logger.error("Failed to send error response", ioException);
                                }
                            }
                        })
                        .exceptionally(ex -> {
                            logger.error("Error fetching scan list for user: {}", username, ex);
                            try {
                                ctx.respond("Failed to fetch scans for user: " + username + ". Reason: " + ex.getMessage());
                            } catch (IOException e) {
                                logger.error("Failed to send error response", e);
                            }
                            return null;
                        });

            }else if ("scan_summary".equalsIgnoreCase(subCommand)) {
                if (commandValue.isEmpty()) {
                    return ctx.ack("Please provide a scan ID. Usage: `/appscan scan_summary <ID>`");
                }
                ctx.ack();
                String scanId = commandValue;
                appScanService.getScanSummaryById(scanId)
                        .thenAccept(optDetails -> {
                            try {
                                if (optDetails.isEmpty()) {
                                    ctx.respond("No scan found for ID: " + scanId);
                                } else {
                                    FullScanDetails details = optDetails.get();
                                    List<LayoutBlock> blocks = notificationService.buildScanCompletionBlocks(details);
                                    ctx.respond(r -> r.responseType("in_channel").blocks(blocks));
                                }
                            } catch (IOException e) {
                                logger.error("Failed to respond with scan summary for ID: {}", scanId, e);
                                try {
                                    ctx.respond("Failed to fetch scan summary for ID: " + scanId + ". Reason: " + e.getMessage());
                                } catch (IOException ioException) {
                                    logger.error("Failed to send error response", ioException);
                                }
                            }
                        })
                        .exceptionally(ex -> {
                            logger.error("Error fetching scan summary for ID: {}", scanId, ex);
                            try {
                                ctx.respond("Failed to fetch scan summary for ID: " + scanId + ". Reason: " + ex.getMessage());
                            } catch (IOException e) {
                                logger.error("Failed to send error response", e);
                            }
                            return null;
                        });
            }
            else {
                return ctx.ack("Sorry, I didn't understand that command. Try `/appscan help`.");
            }
            return ctx.ack();
        });
        return app;
    }
}
