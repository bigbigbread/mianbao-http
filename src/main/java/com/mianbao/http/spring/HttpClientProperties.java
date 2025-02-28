package com.mianbao.http.spring;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties("mianbao.http.base-packages")
public class HttpClientProperties {
    private List<String> basePackages;
}
