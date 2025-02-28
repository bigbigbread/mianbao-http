package com.mianbao.http.spring;

import com.mianbao.http.annotation.HttpClient;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

public class HttpClientFactoryBean<T> implements FactoryBean<T> {
    private final Class<T> interfaceClass;
    private final String baseUrl;
    
    public HttpClientFactoryBean(Class<T> interfaceClass, String baseUrl) {
        this.interfaceClass = interfaceClass;
        this.baseUrl = baseUrl;
    }
    
    @Override
    public T getObject() throws Exception {
        Object obj = Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class[]{interfaceClass},
                new HttpClientInvocationHandler(interfaceClass, baseUrl)
        );
        return interfaceClass.cast(obj);
    }
    
    @Override
    public Class<?> getObjectType() {
        return interfaceClass;
    }
    
    private static class HttpClientInvocationHandler implements InvocationHandler {
        private static final int DEFAULT_TIMEOUT = 30;
        private final OkHttpClient client;
        private final String baseUrl;
        
        public HttpClientInvocationHandler(Class<?> interfaceClass, String baseUrl) {
            HttpClient anno = interfaceClass.getAnnotation(HttpClient.class);
            TimeUnit timeUnit = anno.callTimeoutUnit();
            int timeout = anno.callTimeout();
            timeout = timeout < 0 ? DEFAULT_TIMEOUT : timeout;
            
            this.client = new OkHttpClient.Builder()
                    .callTimeout(timeout, timeUnit)
                    .build();
            this.baseUrl = baseUrl;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // todo
            return "测试 Spring 动态注册是否正常运作";
        }
    }
}
