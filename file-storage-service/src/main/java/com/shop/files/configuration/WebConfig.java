package com.shop.files.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebFluxConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations(
                        "classpath:/static/images/",
                        "file:./uploads/"
                )
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS));
    }
}
