package com.mianbao.http.annotation;

import com.mianbao.http.spring.HttpClientRegistrar;
import org.springframework.context.annotation.Import;

@Import({HttpClientRegistrar.class})
public @interface EnableHttpClient {
    
    String[] basePackages() default {};
}
