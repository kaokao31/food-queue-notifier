package com.kku.queuenotify.controller.api;

import com.kku.queuenotify.exception.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;

class GlobalExceptionHandlerTest {
  private final MockMvc mvc=MockMvcBuilders.standaloneSetup(new Probe()).setControllerAdvice(new GlobalExceptionHandler()).build();
  @RestController static class Probe {
    record Input(@NotBlank String name){}
    @PostMapping("/probe") void input(@Valid @RequestBody Input value){}
    @GetMapping("/number") void number(@RequestParam int value){}
    @GetMapping("/header") void header(@RequestHeader("X-Queue-Token") String token){}
    @GetMapping("/error/{kind}") void error(@PathVariable String kind) {
      switch(kind) {
        case "missing" -> throw new ApiException(HttpStatus.NOT_FOUND,"Order not found");
        case "state" -> throw new IllegalStateTransitionException("Queue state does not match this operation");
        case "database" -> throw new DataIntegrityViolationException("SQL secret-token private-key");
        case "denied" -> throw new AccessDeniedException("private-key");
        case "login" -> throw new BadCredentialsException("secret-password");
        case "push" -> throw new PushDemoException(HttpStatus.SERVICE_UNAVAILABLE,"Push is not configured");
        default -> throw new IllegalStateException("SQL secret-token private-key");
      }
    }
  }
  @Test void malformedJsonDoesNotEchoBody() throws Exception {
    mvc.perform(post("/probe").contentType(MediaType.APPLICATION_JSON).content("{secret-token"))
      .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.message").value("Invalid request"))
      .andExpect(header().string("Cache-Control","no-store")).andExpect(content().string(not(containsString("secret-token"))));
  }
  @Test void fieldValidationDoesNotEchoRejectedValues() throws Exception {
    mvc.perform(post("/probe").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
      .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Bad Request")).andExpect(jsonPath("$.message").value("Invalid request"));
  }
  @Test void conversionAndMissingHeadersAreBadRequests() throws Exception {
    mvc.perform(get("/number").param("value","secret-token")).andExpect(status().isBadRequest()).andExpect(content().string(not(containsString("secret-token"))));
    mvc.perform(get("/header")).andExpect(status().isBadRequest()).andExpect(header().string("Cache-Control","no-store"));
  }
  @Test void methodAndContentTypeKeepProtocolStatuses() throws Exception {
    mvc.perform(put("/probe")).andExpect(status().isMethodNotAllowed()).andExpect(header().string("Allow","POST")).andExpect(jsonPath("$.status").value(405));
    mvc.perform(post("/probe").contentType(MediaType.TEXT_PLAIN).content("secret-token")).andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.status").value(415));
  }
  @Test void businessStatusesAndSafeMessagesArePreserved() throws Exception {
    mvc.perform(get("/error/missing")).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Order not found"));
    mvc.perform(get("/error/state")).andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
    mvc.perform(get("/error/push")).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.message").value("Push is not configured"));
  }
  @Test void databaseAndUnexpectedErrorsHideDetails() throws Exception {
    mvc.perform(get("/error/database")).andExpect(status().isConflict()).andExpect(content().string(not(containsString("SQL"))));
    mvc.perform(get("/error/unexpected")).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.message").value("Internal server error"))
      .andExpect(header().string("Cache-Control","no-store")).andExpect(content().string(not(containsString("private-key"))));
  }
  @Test void mvcSecurityExceptionsUseSameContract() throws Exception {
    mvc.perform(get("/error/login")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401)).andExpect(jsonPath("$.error").value("Unauthorized"));
    mvc.perform(get("/error/denied")).andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("Access denied"));
  }
}
