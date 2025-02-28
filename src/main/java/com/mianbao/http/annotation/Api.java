package com.mianbao.http.annotation;

import com.mianbao.http.enums.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Api {
    String url() default "";
    
    HttpMethod method() default HttpMethod.GET;
}
