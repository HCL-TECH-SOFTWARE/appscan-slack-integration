package com.hcl.appscan.slackapp;
import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

/**
 * Main entry point for the AppScan Slack App Spring Boot application.
 * <p>
 * Configures and launches the application, enabling scheduling for periodic tasks.
 * </p>
 */
@SpringBootApplication
@EnableScheduling
public class AppScanSlackAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppScanSlackAppApplication.class, args);
    }
}

@Component
class SlackAppRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(SlackAppRunner.class);
    private final App slackApp;
    private final String appToken;

    public SlackAppRunner(App slackApp, @Value("${slack.app.token}") String appToken) {
        this.slackApp = slackApp;
        this.appToken = appToken;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("--- Starting Slack App in Socket Mode ---");
        new SocketModeApp(appToken, slackApp).startAsync();
    }
}


