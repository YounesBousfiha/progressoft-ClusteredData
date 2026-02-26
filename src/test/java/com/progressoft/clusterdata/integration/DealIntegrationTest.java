package com.progressoft.clusterdata.integration;


import com.progressoft.clusterdata.repository.DealRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DealIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("bloomberg_test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DealRepository dealRepository;

    @BeforeEach
    void setUp() {
        dealRepository.deleteAll();
    }

    @Test
    void shouldImportDealsAndSkipDuplicatesInRealDb() throws Exception {
        String jsonPayload = """
                [
                  {
                    "dealUniqueId": "IT-001",
                    "fromCurrency": "USD",
                    "toCurrency": "MAD",
                    "dealTimestamp": "2026-02-26T10:15:30",
                    "dealAmount": 1000.00
                  },
                  {
                    "dealUniqueId": "IT-002",
                    "fromCurrency": "EUR",
                    "toCurrency": "MAD",
                    "dealTimestamp": "2026-02-26T10:16:00",
                    "dealAmount": 2000.00
                  },
                  {
                    "dealUniqueId": "IT-001",
                    "fromCurrency": "USD",
                    "toCurrency": "MAD",
                    "dealTimestamp": "2026-02-26T10:15:30",
                    "dealAmount": 1000.00
                  }
                ]
                """;

        mockMvc.perform(post("/api/v1/deals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReceived").value(3))
                .andExpect(jsonPath("$.successfulImports").value(2))
                .andExpect(jsonPath("$.failedOrSkipped").value(1));
    }

}
