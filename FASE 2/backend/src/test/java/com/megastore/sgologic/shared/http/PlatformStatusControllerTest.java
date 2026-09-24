package com.megastore.sgologic.shared.http;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlatformStatusControllerTest {

    @Test
    void returnsOnlyTechnicalStatusWithoutConfigurationOrSecrets() throws Exception {
        String correlationId = UUID.randomUUID().toString();
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new PlatformStatusController("sgo-logic"))
                .addFilter(new CorrelationIdFilter())
                .build();

        mvc.perform(get("/api/v1/platform/status")
                        .header(CorrelationIdFilter.HEADER_NAME, correlationId))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.HEADER_NAME, correlationId))
                .andExpect(jsonPath("$.applicationName").value("sgo-logic"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.timestamp", matchesPattern("^\\d{4}-\\d{2}-\\d{2}T.*Z$")))
                .andExpect(jsonPath("$.correlationId").value(correlationId))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.environment").doesNotExist())
                .andExpect(jsonPath("$.datasource").doesNotExist());
    }
}
