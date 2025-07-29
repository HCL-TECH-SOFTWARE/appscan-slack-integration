# **HCL AppScan Slack Integration**

This application provides a powerful integration between **HCL AppScan on Cloud (ASoC)** or **HCL AppScan 360°** and your Slack workspace. It is designed to bring real-time security insights and on-demand application summaries directly into your team's collaborative environment, fostering a seamless DevSecOps culture.

The app runs in **Socket Mode**, which means it establishes a secure WebSocket connection to Slack and does not require you to expose public HTTP endpoints, making it ideal for running securely behind a corporate firewall.

## **Features**

### **Proactive Notifications**

* **Automated Scan Completion Alerts:** Receive instant, proactive alerts when a scan for a monitored application is completed.  
* **Detailed Summaries:** Notifications include a rich summary with:  
  * Application Name and Scan Name  
  * Scan Technology (DAST, SAST, etc.)  
  * A full breakdown of issue counts with color-coded emojis: ⚫ Critical, 🔴 High, 🟠 Medium, 🔵 Low, and ⚪️ Informational.  
  * Details of the user who initiated the scan (Full Name, Username, Email).  
  * A timestamp for when the scan was created.  
  * A direct "View in AppScan" button to the full report.  
* **Targeted App-to-Channel Mapping:** Configure specific applications to send notifications to one or more designated Slack channels, ensuring the right teams get the right alerts.

### **On-Demand Slash Commands**

Empower any team member to get real-time security data without leaving Slack.

* /appscan summary \<Application Name\>: Get a comprehensive security overview of a specific application.  
* /appscan list\_apps: Display a formatted list of all applications in your AppScan instance, with their current risk rating and total issue counts.  
* /appscan list\_my\_scans \<AppScan\_Username\>: List all scans initiated by a specific user.  
* /appscan scan\_summary \<Scan\_ID\>: Get a detailed summary for a specific scan by its ID.  
* /appscan help: Show a list of all available commands.

### **Security & Deployment**

* **Self-Hosted Model:** You clone, configure, and run the application on your own infrastructure, ensuring your credentials and data remain within your control.  
* **Socket Mode:** Uses a secure WebSocket connection for communication with Slack, eliminating the need for public endpoints and tools like ngrok.  
* **Configurable for Testing:** Includes an optional flag to allow connections to servers with untrusted SSL/TLS certificates for development or testing environments.

## **Setup and Installation Guide**

Follow these steps to set up and run the integration in your environment.

### **Part 1: Prerequisites**

Ensure you have the following software installed:

* Java (JDK) 17 or newer  
* Maven 3.8 or newer  
* Git

### **Part 2: Clone the Repository**

Open a terminal and clone the application source code from the official HCL-TECH-SOFTWARE GitHub repository.

git clone https://github.com/HCL-TECH-SOFTWARE/appscan-slack-integration.git  
cd appscan-slack-integration

### **Part 3: Create and Configure Your Slack App**

1. Go to the [Slack API Dashboard](https://api.slack.com/apps) and click **"Create New App"**.  
2. Choose the **"From a manifest"** option.  
3. Select the workspace where you want to install the app and click **Next**.  
4. In the "Enter manifest below" section, choose the **JSON** tab and paste the entire content of the slack-manifest.json file from the project.  
5. Review the manifest details and click **Next**.  
6. Click **Create** to finish creating the app.

### **Part 4: Generate Slack Tokens**

You will need two types of tokens from your new Slack app's dashboard.

**A. Get the App-Level Token (for Socket Mode):**

1. In the left sidebar, go to **Settings** \-\> **Basic Information**.  
2. Scroll down to the **App-Level Tokens** section.  
3. Click **Generate Token and Scopes**.  
4. Give the token a name (e.g., appscan-socket-token).  
5. Click **Add Scope** and select connections:write.  
6. Click **Generate**.  
7. Copy the token that starts with **xapp-**. You will need this for the slack.app.token property.

**B. Get the Bot Token (for API Calls):**

1. In the left sidebar, go to **Settings** \-\> **Install App**.  
2. Click the **Install to Workspace** button.  
3. Follow the prompts to authorize the app.  
4. After authorization, you will be redirected to the **OAuth & Permissions** page.  
5. Copy the **Bot User OAuth Token**. It will start with **xoxb-**. You will need this for the slack.bot.token property.

### **Part 5: Configure the Application**

All configuration is handled in the src/main/resources/application.properties file.

1. Open the application.properties file in your editor.  
2. Fill in the placeholder values:  
   \# Slack App Credentials  
   slack.bot.token=PASTE\_YOUR\_BOT\_TOKEN\_HERE\_(xoxb-...)  
   slack.signing.secret=PASTE\_YOUR\_SIGNING\_SECRET\_HERE  
   slack.app.token=PASTE\_YOUR\_APP\_LEVEL\_TOKEN\_HERE\_(xapp-...)

   \# AppScan API Configuration  
   appscan.api.baseurl=https://cloud.appscan.com  
   appscan.api.key=PASTE\_YOUR\_APPSCAN\_KEY\_ID\_HERE  
   appscan.api.secret=PASTE\_YOUR\_APPSCAN\_KEY\_SECRET\_HERE

   \# Notification Configuration  
   \# Comma-separated list of AppScan applications to monitor for scan completion  
   appscan.monitored.apps=MyWebApp,E-Commerce Portal

   \# Comma-separated list of Slack channels to map to AppScan applications  
   \# Format: AppName1:\#channel-a,\#channel-b;AppName2:\#channel-c  
   appscan.app.channel.mapping=MyWebApp:\#appsec-alerts;E-Commerce Portal:\#dev-team

   \# Polling Configuration (in milliseconds)  
   appscan.poller.rate.ms=60000

   \# Testing Configuration  
   \# WARNING: Setting this to true bypasses all SSL certificate checks.  
   \# Do NOT use in production.  
   appscan.allowUntrusted=false

   \# Server Port Configuration  
   server.port=8080

### **Part 6: Build and Run the Application**

1. Build the App:  
   Open a terminal in the project's root directory and run:  
   mvn clean package

2. Run the App:  
   Once the build is complete, run the application:  
   java \-jar target/appscan-slack-app-0.0.1-SNAPSHOT.jar

   The application will start and automatically connect to Slack using Socket Mode. You do not need to use ngrok.

### **Part 7: Add the Bot to Channels**

The final step is to invite your bot into the Slack channels where you want to use it or receive notifications. In each relevant channel, type @YourBotName and press Enter, then click to invite it.

## **Usage Guide**

* Get an Application Summary:  
  /appscan summary "My Web Application"  
* List Applications:  
  /appscan list\_apps  
* List Scans Started by a User:  
  /appscan list\_my\_scans "john.doe@example.com"  
* Get a Specific Scan's Summary:  
  /appscan scan\_summary "d4a3b2c1-e8f9-1234-abcd-5f6e7d8c9b0a"
* Get Help:  
  /appscan help