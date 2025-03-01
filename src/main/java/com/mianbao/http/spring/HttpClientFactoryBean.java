package com.mianbao.http.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianbao.http.annotation.Api;
import com.mianbao.http.annotation.HttpClient;
import com.mianbao.http.enums.HttpMethod;
import com.mianbao.http.exception.HttpClientException;
import okhttp3.*;
import org.springframework.beans.factory.FactoryBean;

import java.io.InputStream;
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
    public T getObject() {
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
        private final ObjectMapper objectMapper;
        
        public HttpClientInvocationHandler(Class<?> interfaceClass, String baseUrl) {
            HttpClient anno = interfaceClass.getAnnotation(HttpClient.class);
            TimeUnit timeUnit = anno.callTimeoutUnit();
            int timeout = anno.callTimeout();
            timeout = timeout < 0 ? DEFAULT_TIMEOUT : timeout;
            
            this.client = new OkHttpClient.Builder()
                    .callTimeout(timeout, timeUnit)
                    .build();
            this.baseUrl = baseUrl;
            this.objectMapper = new ObjectMapper();
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // todo
            Api api = method.getAnnotation(Api.class);
            if (api == null) {
                throw new HttpClientException("该方法缺少 @Api 注解, 请使用 @Api 注解配置调用信息");
            }
            
            String subUrl = api.url();
            HttpMethod httpMethod = api.method();
            
            String url = baseUrl + subUrl;
            
            Object arg = args[0];
            byte[] bytes = objectMapper.writeValueAsBytes(arg);
            
            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(bytes, mediaType);
            
            Request request = new Request.Builder()
                    .url(url)
                    .method(httpMethod.name(), body)
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                InputStream in = response.body().byteStream();
                Class<?> returnTypeClass = method.getReturnType();
                Object res = objectMapper.readValue(in, returnTypeClass);
                
                return res;
            }
        }
    }
}
