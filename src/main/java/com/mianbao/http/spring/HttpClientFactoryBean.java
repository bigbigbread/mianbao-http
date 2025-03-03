package com.mianbao.http.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianbao.http.annotation.Api;
import com.mianbao.http.annotation.HttpClient;
import com.mianbao.http.annotation.PathVariable;
import com.mianbao.http.annotation.RequestParam;
import com.mianbao.http.enums.HttpMethod;
import com.mianbao.http.exception.HttpClientException;
import okhttp3.*;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.web.util.UriTemplate;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

public class HttpClientFactoryBean<T> implements FactoryBean<T> {
    
    private final Class<T> interfaceClass;
    
    public HttpClientFactoryBean(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }
    
    @Override
    public T getObject() {
        Object obj = Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class[]{interfaceClass},
                new HttpClientInvocationHandler(interfaceClass)
        );
        return interfaceClass.cast(obj);
    }
    
    @Override
    public Class<?> getObjectType() {
        return interfaceClass;
    }
    
    private static class HttpClientInvocationHandler implements InvocationHandler {
        
        private static final int DEFAULT_TIMEOUT = 30;
        
        private final String baseUrl;
        
        private final OkHttpClient client;
        
        private final ObjectMapper objectMapper;
        
        public HttpClientInvocationHandler(Class<?> interfaceClass) {
            HttpClient anno = interfaceClass.getAnnotation(HttpClient.class);
            
            // baseHttpUrl
            this.baseUrl = anno.baseUrl();
            
            // timeoutUnit, timeout
            TimeUnit timeUnit = anno.callTimeoutUnit();
            int timeout = anno.callTimeout();
            timeout = timeout < 0 ? DEFAULT_TIMEOUT : timeout;
            
            // client
            this.client = new OkHttpClient.Builder()
                    .callTimeout(timeout, timeUnit)
                    .build();
            
            // objectMapper
            this.objectMapper = new ObjectMapper();
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // todo
            Api api = method.getAnnotation(Api.class);
            if (api == null) {
                throw new HttpClientException("该方法缺少 @Api 注解, 请使用 @Api 注解配置调用信息");
            }
            
            // url
            String subUrl = api.url();
            UriTemplate uriTemplate = new UriTemplate(baseUrl + subUrl);
            
            // parameter
            RequestBody requestBody = null;
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            for (int i = 0; i < args.length; i++) {
                Annotation[] annotations = parameterAnnotations[i];
                Object arg = args[i];
                for (Annotation anno : annotations) {
                    if (anno instanceof com.mianbao.http.annotation.RequestBody) { // 请求体
                        byte[] bytes = objectMapper.writeValueAsBytes(arg);
                        requestBody = RequestBody.create(bytes, MediaType.parse("application/json"));
                    } else if (anno instanceof PathVariable) { // 路径参数
                        // todo
                    } else if (anno instanceof RequestParam) { // url 参数
                        // todo
                    } else {
                        throw new HttpClientException("抱歉, 暂不支持:" + anno.getClass().getName());
                    }
                }
            }
            
            HttpMethod httpMethod = api.method();
            Request request = new Request.Builder()
                    .url(uriTemplate.toString())
                    .method(httpMethod.name(), requestBody)
                    .build();
            
            // 发请求
            try (Response response = client.newCall(request).execute()) {
                InputStream in = response.body().byteStream();
                Class<?> returnTypeClass = method.getReturnType();
                Object res = objectMapper.readValue(in, returnTypeClass);
                
                return res;
            }
        }
    }
}
