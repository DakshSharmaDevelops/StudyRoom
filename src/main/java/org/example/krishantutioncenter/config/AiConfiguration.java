package org.example.krishantutioncenter.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiFeatureProperties.class)
public class AiConfiguration {
}
