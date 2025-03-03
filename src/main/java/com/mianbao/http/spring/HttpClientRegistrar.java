package com.mianbao.http.spring;

import com.mianbao.http.annotation.EnableHttpClient;
import com.mianbao.http.annotation.HttpClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
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
        log.info("开始注册HTTP客户端...");
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
        String[] basePackageArray1 = (String[]) enableHttpClientAttributes.get("basePackages");
        
        // 对 basePackages 去重
        Set<String> basePackages = new HashSet<>(Arrays.asList(basePackageArray1));
        String basePackageArray2Str = environment.getProperty("mianbao.http.base-packages"); // 外部配置
        if (basePackageArray2Str != null && !basePackageArray2Str.isBlank()) {
            // 处理空格
            basePackageArray2Str = basePackageArray2Str.replaceAll(" +", "");
            
            // 分隔
            String[] basePackageArray2 = basePackageArray2Str.split(",");
            
            basePackages.addAll(Arrays.asList(basePackageArray2));
        }
        
        // 扫描包, 并注册 BeanDefinition
        try {
            int count = 0;
            for (String basePackage : basePackages) {
                Set<BeanDefinition> httpClientCandidateComponents = provider.findCandidateComponents(basePackage);
                for (BeanDefinition httpClientBeanDefinition : httpClientCandidateComponents) {
                    // 取接口 Class<?>
                    String httpClientClassName = httpClientBeanDefinition.getBeanClassName();
                    Class<?> httpClientClass = Class.forName(httpClientClassName);
                    
                    // 构建 HttpClientFactoryBean 的 BeanDefinition
                    BeanDefinition httpClientWrapperBeanDefinition = BeanDefinitionBuilder.rootBeanDefinition(HttpClientFactoryBean.class)
                            .addConstructorArgValue(httpClientClass)
                            .getBeanDefinition();
                    
                    // 注册 BeanDefinition
                    String httpClientBeanName = httpClientClass.getSimpleName();
                    registry.registerBeanDefinition(httpClientBeanName, httpClientWrapperBeanDefinition);
                    
                    log.info("注册HTTP客户端: {}", httpClientClassName);
                }
                count += httpClientCandidateComponents.size();
            }
            log.info("总共注册了 {} 个HTTP客户端", count);
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage(), e);
        }
    }
    
    @Override
    public void setEnvironment(@NotNull Environment environment) {
        this.environment = environment;
    }
}
