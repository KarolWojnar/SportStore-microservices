package com.shop.files.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.config.ResourceHandlerRegistration;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;

import static org.mockito.Mockito.*;

class WebConfigTest {

    @Test
    void addResourceHandlers_shouldConfigureResourceHandlers() {
        WebConfig webConfig = new WebConfig();
        ResourceHandlerRegistry registry = mock(ResourceHandlerRegistry.class);
        ResourceHandlerRegistration registration = mock(ResourceHandlerRegistration.class);

        when(registry.addResourceHandler(any())).thenReturn(registration);
        when(registration.addResourceLocations(any(String[].class))).thenReturn(registration);

        webConfig.addResourceHandlers(registry);

        verify(registry).addResourceHandler("/images/**");
        verify(registry).addResourceHandler("/uploads/**");
        verify(registration, times(2)).setCacheControl(any());
    }
}