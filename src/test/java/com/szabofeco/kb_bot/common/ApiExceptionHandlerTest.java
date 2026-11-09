package com.szabofeco.kb_bot.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTest {

    @RestController
    static class TestController {

        record Body(@NotBlank String question, Product product) {
        }

        @GetMapping("/param")
        String param(@RequestParam @NotBlank String question, @RequestParam(required = false) Product product) {
            return "ok";
        }

        @PostMapping("/body")
        String body(@Valid @RequestBody Body body) {
            return "ok";
        }

        @GetMapping("/client-error")
        String clientError() {
            throw new ErrorResponseException(HttpStatus.BAD_REQUEST,
                    ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Missing required frontmatter field: id"),
                    null);
        }

        @GetMapping("/server-error")
        String serverError() {
            throw new ErrorResponseException(HttpStatus.BAD_GATEWAY,
                    ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "Please try again later."),
                    new IllegalStateException("internal details"));
        }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    @Test
    void parameterValidationListsTheViolations() throws Exception {
        mvc.perform(get("/param").param("question", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("question: must not be blank"));
    }

    @Test
    void bodyValidationListsTheViolations() throws Exception {
        mvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON).content("{\"question\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("question: must not be blank"));
    }

    @Test
    void unknownEnumInTheBodyNamesTheAllowedProducts() throws Exception {
        mvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"x\", \"product\": \"AUTOHITEL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("LAKASHITEL")));
    }

    @Test
    void unknownEnumParameterNamesTheAllowedValues() throws Exception {
        mvc.perform(get("/param").param("question", "x").param("product", "AUTOHITEL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "Invalid value for parameter 'product': AUTOHITEL. Allowed values: "
                                + "[LAKASHITEL, SZEMELYI_KOLCSON, BANKSZAMLA]"));
    }

    @Test
    void missingParameterUsesTheFrameworkDefault() throws Exception {
        mvc.perform(get("/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("question")));
    }

    @Test
    void errorResponseExceptionsKeepTheirOwnStatusAndDetail() throws Exception {
        mvc.perform(get("/client-error"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Missing required frontmatter field: id"));
        mvc.perform(get("/server-error"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("Please try again later."));
    }
}
