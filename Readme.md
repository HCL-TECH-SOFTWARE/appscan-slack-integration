# **HCL AppScan Slack integration**

This application integrates **HCL AppScan on Cloud (ASoC)** or **HCL AppScan 360°** with your Slack workspace. It brings real-time security insights and on-demand application summaries directly into your team's chat, helping you integrate security into your development process.

The app runs in **Socket Mode**, which establishes a secure WebSocket connection to Slack. This approach doesn't require you to expose public HTTP endpoints, so you can run it securely behind a corporate firewall.

## **Features**

### **Proactive notifications**

* **Automated scan completion alerts:** Receive an instant alert when a scan for a monitored application is complete.  
* **Detailed summaries:** Notifications include a rich summary with:  
  * Application Name and Scan Name  
  * Scan Technology (DAST, SAST, SCA)  
  * A full breakdown of issue counts: ⚫ Critical, 🔴 High, 🟠 Medium, 🔵 Low, and ⚪️ Informational.  
  * Details of the person who initiated the scan (Full Name, Username, Email).  
  * A timestamp for when the scan was created.  
  * A **View in AppScan** button that links to the full report.  
* **Targeted app-to-channel mapping:** You can configure specific applications to send notifications to one or more Slack channels, which ensures the right teams get the right alerts.

### **On-demand slash commands**

Any team member to get real-time security data without leaving Slack.

* /appscan summary \<Application Name\>: Get a comprehensive security overview of a specific application.  
* /appscan list\_apps: Display a formatted list of all applications in your AppScan instance with their current risk rating and total issue counts.  
* /appscan list\_scans \<AppScan\_Email\>: List all scans initiated by a specific user's email address.  
* /appscan scan\_summary \<Scan\_ID\>: Get a detailed summary for a specific scan by its ID.  
* /appscan help: Show a list of all available commands.

### **Security and deployment**

* **Self-hosted model:** You clone, configure, and run the application on your infrastructure, which ensures your credentials and data remain within your control.  
* **Socket Mode:** Uses a secure WebSocket connection for communication with Slack, so you don't need public endpoints or tools like ngrok.  
* **Configurable for testing:** Includes an optional flag to allow connections to servers with untrusted SSL/TLS certificates for development or testing.

## **Setup and installation guide**

Follow these steps to set up and run the integration in your environment.

### **Part 1: Prerequisites**

Ensure you have the following software installed:

* Java (JDK) 17 or newer  
* Maven 3.8 or newer  
* Git

### **Part 2: Clone the repository**

Open a terminal and clone the application source code from the official HCL-TECH-SOFTWARE GitHub repository.

git clone [https://github.com/HCL-TECH-SOFTWARE/appscan-slack-integration.git](https://github.com/HCL-TECH-SOFTWARE/appscan-slack-integration.git)  
cd appscan-slack-integration

### **Part 3: Create and configure your Slack app**

1. Go to the [Slack API Dashboard](https://api.slack.com/apps) and click **Create New App**.  
2. Choose the **From a manifest** option.  
3. Select the workspace where you want to install the app and click **Next**.  
4. In the Enter manifest below section, select the **JSON** tab and paste the entire content of the slack-manifest.json file from the project.  
5. Review the manifest details and click **Next**.  
6. Click **Create** to finish creating the app.

### **Part 4: Generate Slack tokens**

You need two types of tokens from your new Slack app's dashboard.

**A. Get the app-level token for Socket Mode:**

1. On the left sidebar, go to **Settings** \-\> **Basic Information**.  
2. Scroll down to the **App-Level Tokens** section.  
3. Click **Generate Token and Scopes**.  
4. Give the token a name (e.g., appscan-socket-token).  
5. Click **Add Scope** and select connections:write.  
6. Click **Generate**.  
7. Copy the token that starts with **xapp-**. You will need this for the slack.app.token property.

**B. Get the bot token for API calls:**

1. On the left sidebar, go to **Settings** \-\> **Install App**.  
2. Click the **Install to Workspace** button.  
3. Follow the prompts to authorize the app.  
4. After you authorize the app, you will be redirected to the **OAuth & Permissions** page.  
5. Copy the **Bot User OAuth Token**. It will start with **xoxb-**. You will need this for the slack.bot.token property.

### **Part 5: Configure the application**

All configuration is handled in the src/main/resources/application.properties file.

1. Open the application.properties file in your editor.  
     
2. Enter the placeholder values:  
   \# Slack App Credentials  
   slack.bot.token=PASTE\_YOUR\_BOT\_TOKEN\_HERE\_(xoxb-...)  
   slack.signing.secret=PASTE\_YOUR\_SIGNING\_SECRET\_HERE  
   slack.app.token=PASTE\_YOUR\_APP\_LEVEL\_TOKEN\_HERE\_(xapp-...)  
     
   \# AppScan API Configuration  
   appscan.api.baseurl=<AppScan API Base URL> 
   appscan.api.key=PASTE\_YOUR\_APPSCAN\_KEY\_ID\_HERE  
   appscan.api.secret=PASTE\_YOUR\_APPSCAN\_KEY\_SECRET\_HERE  
     

   
  \# Notification Configuration
  \# Map AppScan applications to Slack channels
  \# The format is: AppName1:\#channel-a,\#channel-b;AppName2:\#channel-c  
  
   appscan.app.channel.mapping=AppName1:\#channel-a,\#channel-b;AppName2:\#channel-c 
  
  \# Map AppScan applications to Slack user IDs
  \# The format is: <AppScan Application Name>:<Slack User ID 1>,<Slack User ID 2>
  \# Multiple users can be specified for the same application, separated by commas.
  appscan.app.user.mapping=Test_App:Slack_MemberID1,Slack_MemberID2;Test:Slack_MemberID3

   \# Polling Configuration (in milliseconds)  
   appscan.poller.rate.ms=60000  
     
   \# Testing Configuration  
   \# WARNING: Setting this to true bypasses all SSL certificate checks.  
   \# Do NOT use in production.  
   appscan.allowUntrusted=false  
     
   \# Server Port Configuration  
   server.port=8080

### **Part 6: Build and run the application**

1. Build the app:  
   Open a terminal in the project's root directory and run:  
   mvn clean package  
     
2. Run the app:  
   Once the build is complete, run the application:  
   java \-jar target/appscan-slack-app-0.0.1-SNAPSHOT.jar  
     
   The application will start and automatically connect to Slack using Socket Mode. You do not need to use ngrok.

### **Part 7: Add the bot to channels**

The final step is to invite your bot into the Slack channels where you want to use it or receive notifications. In each relevant channel, type @YourBotName and press **Enter**, then click to invite it.

## **Usage guide**

Note: Application names that contain spaces must be enclosed in double quotes

* Get an Application Summary:  
  /appscan summary "My Web Application"  
* List Applications:  
  /appscan list\_apps  
* List Scans Started by a User:  
  /appscan list\_scans "[john.doe@example.com](mailto:john.doe@example.com)"  
* Get a Specific Scan's Summary:  
  /appscan scan\_summary "d4a3b2c1-e8f9-1234-abcd-5f6e7d8c9b0a"  
* Get Help:  
  /appscan help

**Troubleshooting**

* **Problem**: The /appscan command returns a "not found" error in Slack.  
  * Solution: Ensure you have successfully installed the app in your workspace from the Settings \-\> Install App page in your Slack App's dashboard.


* **Problem**: The bot does not respond in a specific channel.  
  * Solution: You must invite the bot into each channel where you want to use it. Type @YourBotName in the channel and follow the prompt to invite it.

* **Problem**: Notifications are not being received for a monitored application.  
  * Solution: Double-check that the application name in appscan.monitored.apps and appscan.app.channel.mapping exactly matches the name in AppScan. Also, ensure the bot has been invited to the destination channels.