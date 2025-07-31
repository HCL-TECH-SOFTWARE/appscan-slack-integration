
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