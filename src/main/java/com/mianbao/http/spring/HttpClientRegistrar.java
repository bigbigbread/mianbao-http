package com.mianbao.http.spring;

import com.mianbao.http.annotation.EnableHttpClient;
import com.mianbao.http.annotation.HttpClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.*;

public class HttpClientRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {
    
    private static final Logger log = LoggerFactory.getLogger(HttpClientRegistrar.class);
    
    private Environment environment;
    
    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, @NotNull BeanDefinitionRegistry registry) {
        log.info("HttpClientRegistrar 已成功调用");
        // 扫描 @HttpClient 注解标记的接口
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isInterface(); // 只拿接口
            }
        };
        provider.addIncludeFilter(new AnnotationTypeFilter(HttpClient.class)); // 只拿有 @HttpClient 注解的接口
        
        // 拿 @EnableHttpClient 注解配置的包
        Map<String, Object> enableHttpClientAttributes = metadata.getAnnotationAttributes(EnableHttpClient.class.getName());
        assert enableHttpClientAttributes != null; // 如果此类被加载, 那说明 @EnableHttpClient 注解肯定被用户使用了
        String[] basePackageArray = (String[]) enableHttpClientAttributes.get("basePackages");
        
        // 对 basePackages 去重
        Set<String> basePackages = new HashSet<>(Arrays.asList(basePackageArray));
        HttpClientProperties httpClientProperties = environment.getProperty("mianbao.http", HttpClientProperties.class); // 外部配置
        // bug httpClientProperties为null
        if (httpClientProperties != null) {
            List<String> basePackagesList = httpClientProperties.getBasePackages();
            basePackages.addAll(basePackagesList);
        }
        
        // 扫描包, 并注册 BeanDefinition
        try {
            for (String basePackage : basePackages) {
                for (BeanDefinition httpClientBeanDefinition : provider.findCandidateComponents(basePackage)) {
                    // 取接口 Class<?>
                    String httpClientClassName = httpClientBeanDefinition.getBeanClassName();
                    Class<?> httpClientClass = Class.forName(httpClientClassName);
                    
                    // 取 baseUrl
                    HttpClient httpClientAnno = httpClientClass.getAnnotation(HttpClient.class);
                    String pureBaseUrl = httpClientAnno.baseUrl();
                    String baseUrl = environment.getProperty(pureBaseUrl, pureBaseUrl);
                    
                    // 构建 HttpClientFactoryBean 的 BeanDefinition
                    BeanDefinition httpClientFactoryBeanDefinition = BeanDefinitionBuilder.rootBeanDefinition(HttpClientFactoryBean.class)
                            .addConstructorArgValue(httpClientClass)
                            .addConstructorArgValue(baseUrl)
                            .getBeanDefinition();
                    
                    // 注册 BeanDefinition
                    String httpClientBeanName = httpClientClass.getSimpleName();
                    registry.registerBeanDefinition(httpClientBeanName, httpClientFactoryBeanDefinition);
                }
            }
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage(), e);
        }
    }
    
    @Override
    public void setEnvironment(@NotNull Environment environment) {
        this.environment = environment;
    }
}
