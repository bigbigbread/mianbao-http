package com.mianbao.http.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpClient {
    
    String baseUrl();
    
    TimeUnit callTimeoutUnit() default TimeUnit.SECONDS;
    
    int callTimeout() default 30;
}
