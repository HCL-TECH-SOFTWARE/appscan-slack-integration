
package com.hcl.appscan.slackapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import java.util.*;

@Configuration
public class UserAppMappingConfig {
    private final Map<String, List<String>> appUserMapping = new HashMap<>();

    public UserAppMappingConfig(@Value("${appscan.app.user.mapping:}") String mapping) {
        if (mapping != null && !mapping.isEmpty()) {
            String[] appMappings = mapping.split(";");
            for (String appMapping : appMappings) {
                String[] parts = appMapping.split(":");
                if (parts.length == 2) {
                    String appName = parts[0].trim();
                    List<String> users = Arrays.asList(parts[1].split(","));
                    appUserMapping.put(appName, users);
                }
            }
        }
    }

    public List<String> getUsersForApp(String appName) {
        return appUserMapping.getOrDefault(appName, Collections.emptyList());
    }
}