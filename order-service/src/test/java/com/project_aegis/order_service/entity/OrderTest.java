package com.project_aegis.order_service.entity;

import jakarta.persistence.Version;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Order Entity Unit Tests")
class OrderTest {

    @Test
    @DisplayName("Order entity should have @Version annotation on version field")
    void shouldHaveVersionAnnotation() throws NoSuchFieldException {
        Field versionField = Order.class.getDeclaredField("version");

        assertThat(versionField).isNotNull();
        assertThat(versionField.getType()).isEqualTo(Integer.class);
        assertThat(versionField.isAnnotationPresent(Version.class)).isTrue();
    }

    @Test
    @DisplayName("Order should allow setting and getting version")
    void shouldSetAndGetVersion() {
        Order order = Order.builder()
                .version(1)
                .build();

        assertThat(order.getVersion()).isEqualTo(1);

        order.setVersion(2);
        assertThat(order.getVersion()).isEqualTo(2);
    }
}
