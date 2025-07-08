package com.xhk.grpc.config;


import com.xhk.grpc.annotation.GrpcClient;
import com.xhk.grpc.factory.GrpcClientFactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.Map;
import java.util.Set;

public class GrpcClientRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(EnableGrpcClients.class.getName());
        String[] basePackages = (String[]) attributes.get("basePackages");
        if (basePackages.length == 0) {
            String className = importingClassMetadata.getClassName();
            String basePackage = ClassUtils.getPackageName(className);
            basePackages = new String[]{basePackage};
        }


        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isInterface();
            }
        };
        scanner.addIncludeFilter(new AnnotationTypeFilter(GrpcClient.class));

        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
            for (BeanDefinition candidate : candidates) {
                String className = candidate.getBeanClassName();
                try {
                    Class<?> clazz = ClassUtils.forName(className, Thread.currentThread().getContextClassLoader());
                    GrpcClient annotation = clazz.getAnnotation(GrpcClient.class);

                    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(GrpcClientFactoryBean.class);
                    builder.addConstructorArgValue(clazz);
                    builder.addConstructorArgValue(annotation.stub());
                    builder.addConstructorArgValue(annotation.interceptors());
                    builder.addPropertyValue("url", annotation.url());

                    registry.registerBeanDefinition(className, builder.getBeanDefinition());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}