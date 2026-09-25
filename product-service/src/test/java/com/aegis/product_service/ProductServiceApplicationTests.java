package com.aegis.product_service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProductServiceApplicationTests {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private Environment environment;

    @Test
    void contextLoads() {
    }

    @Test
    @DisplayName("server.tomcat.max-http-form-post-size should be 2MB")
    void shouldHaveMaxHttpFormPostSizeConfigured() {
        String maxPostSize = environment.getProperty("server.tomcat.max-http-form-post-size");
        assertThat(maxPostSize).isEqualTo("2MB");
        assertThat(DataSize.parse(maxPostSize)).isEqualTo(DataSize.ofMegabytes(2));
    }

    @Test
    @DisplayName("server.max-http-request-header-size should be 16KB")
    void shouldHaveMaxHttpRequestHeaderSizeConfigured() {
        String maxHeaderSize = environment.getProperty("server.max-http-request-header-size");
        assertThat(maxHeaderSize).isEqualTo("16KB");
        assertThat(DataSize.parse(maxHeaderSize)).isEqualTo(DataSize.ofKilobytes(16));
    }

    @Test
    @DisplayName("spring.servlet.multipart limits should be 2MB")
    void shouldHaveMultipartLimitsConfigured() {
        String maxFileSize = environment.getProperty("spring.servlet.multipart.max-file-size");
        String maxRequestSize = environment.getProperty("spring.servlet.multipart.max-request-size");
        assertThat(maxFileSize).isEqualTo("2MB");
        assertThat(maxRequestSize).isEqualTo("2MB");
    }
}

