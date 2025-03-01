package com.mianbao.http.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;

@ConfigurationProperties("mianbao.http")
public class HttpClientProperties {
    private List<String> basePackages;
    
    public List<String> getBasePackages() {
        return basePackages;
    }
    
    public void setBasePackages(List<String> basePackages) {
        this.basePackages = basePackages == null ? Collections.emptyList() : basePackages;
    }
}
