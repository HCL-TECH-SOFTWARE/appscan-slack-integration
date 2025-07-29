package com.hcl.appscan.slackapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ChannelAppMappingConfig {
    private final Map<String, List<String>> appNameToChannels = new HashMap<>();

    public ChannelAppMappingConfig(@Value("${appscan.app.channel.mapping}") String mapping) {
        if (mapping != null && !mapping.isBlank()) {
            for (String entry : mapping.split(";")) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    String appName = parts[0].trim();
                    List<String> channels = Arrays.asList(parts[1].split(","));
                    appNameToChannels.put(appName, channels.stream().map(String::trim).toList());
                }
            }
        }
    }

    public List<String> getChannelsForApp(String appName) {
        return appNameToChannels.getOrDefault(appName, Collections.emptyList());
    }

    public Set<String> getAllAppNames() {
        return appNameToChannels.keySet();
    }
}