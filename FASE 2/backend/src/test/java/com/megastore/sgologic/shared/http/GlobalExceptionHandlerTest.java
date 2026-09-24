package com.megastore.sgologic.shared.http;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    @Test
    void validationFailureUsesCommonErrorAndDoesNotExposeAStackTrace() throws Exception {
        String correlationId = UUID.randomUUID().toString();
        MockMvc mvc = mvc();

        mvc.perform(post("/test/validation")
                        .header(CorrelationIdFilter.HEADER_NAME, correlationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/test/validation"))
                .andExpect(jsonPath("$.correlationId").value(correlationId))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void malformedJsonUsesTheCommonErrorContract() throws Exception {
        String correlationId = UUID.randomUUID().toString();

        mvc().perform(post("/test/validation")
                        .header(CorrelationIdFilter.HEADER_NAME, correlationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(CorrelationIdFilter.HEADER_NAME, correlationId))
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
                .andExpect(jsonPath("$.correlationId").value(correlationId))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void invalidParameterUsesTheCommonErrorContract() throws Exception {
        String correlationId = UUID.randomUUID().toString();

        mvc().perform(get("/test/parameter")
                        .queryParam("count", "not-a-number")
                        .header(CorrelationIdFilter.HEADER_NAME, correlationId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.correlationId").value(correlationId))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void unexpectedFailureIsSanitized() throws Exception {
        String correlationId = UUID.randomUUID().toString();

        mvc().perform(get("/test/unexpected")
                        .header(CorrelationIdFilter.HEADER_NAME, correlationId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Ocurrió un error interno inesperado."))
                .andExpect(jsonPath("$.correlationId").value(correlationId))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    private MockMvc mvc() {
        return MockMvcBuilders
                .standaloneSetup(new ValidationTestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new CorrelationIdFilter())
                .build();
    }

    @RestController
    @RequestMapping("/test")
    static class ValidationTestController {

        @PostMapping("/validation")
        Input valid(@Valid @RequestBody Input input) {
            return input;
        }

        @GetMapping("/parameter")
        int parameter(@RequestParam int count) {
            return count;
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("sensitive internal detail");
        }
    }

    record Input(@NotBlank String name) {
    }
}
