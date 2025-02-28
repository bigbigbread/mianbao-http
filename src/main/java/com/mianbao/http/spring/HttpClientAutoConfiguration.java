package com.mianbao.http.spring;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({HttpClientProperties.class})
public class HttpClientAutoConfiguration {
}
