package com.project_aegis.order_service.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("DataSource & Hibernate Batching Configuration Tests")
class DataSourceBatchConfigTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("HikariCP pool should be configured with max 25, min-idle 10, and timeout 3000ms")
    void testHikariCPConfiguration() {
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        HikariDataSource hikari = (HikariDataSource) dataSource;

        assertThat(hikari.getMaximumPoolSize()).isEqualTo(25);
        assertThat(hikari.getMinimumIdle()).isEqualTo(10);
        assertThat(hikari.getConnectionTimeout()).isEqualTo(3000L);
    }

    @Test
    @DisplayName("JDBC URL should contain reWriteBatchedInserts=true")
    void testJdbcUrlContainsReWriteBatchedInserts() {
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        HikariDataSource hikari = (HikariDataSource) dataSource;

        assertThat(hikari.getJdbcUrl()).contains("reWriteBatchedInserts=true");
    }

    @Test
    @DisplayName("Hibernate batch insert properties should be configured")
    void testHibernateBatchConfiguration() {
        assertThat(environment.getProperty("spring.jpa.properties.hibernate.jdbc.batch_size"))
                .isEqualTo("50");
        assertThat(environment.getProperty("spring.jpa.properties.hibernate.order_inserts"))
                .isEqualTo("true");
        assertThat(environment.getProperty("spring.jpa.properties.hibernate.order_updates"))
                .isEqualTo("true");
    }
}
