package com.mianbao.http.spring;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@AutoConfiguration
@EnableConfigurationProperties({HttpClientProperties.class})
public class HttpClientAutoConfiguration {
}
