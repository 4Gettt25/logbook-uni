package com.example.logbook.web;

import com.example.logbook.domain.LogEntry;
import com.example.logbook.domain.LogLevel;
import com.example.logbook.service.LogEntryService;
import com.example.logbook.repository.LogEntryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = { LogEntryController.class, RestExceptionHandler.class })
class LogEntryControllerTests {

    @Autowired
    MockMvc mvc;

    @MockBean
    LogEntryService service;

    @MockBean
    LogEntryRepository repository;

    @Test
    void listsLogsWithFilters() throws Exception {
        LogEntry e = new LogEntry();
        e.setId(1L);
        e.setTimestamp(Instant.parse("2024-01-01T00:00:00Z"));
        e.setLogLevel(LogLevel.INFO);
        e.setSource("auth");
        e.setMessage("ok");
        

        Mockito.when(service.search(any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> new PageImpl<>(List.of(e), PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "timestamp")), 1));

        mvc.perform(get("/api/logs")
                        .param("source", "auth"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("auth")));
    }

    @Test
    void returns404WhenNotFound() throws Exception {
        Mockito.when(service.get(42L)).thenThrow(new NoSuchElementException("not found"));

        mvc.perform(get("/api/logs/42"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("not found")));
    }

    @Test
    void validatesCreatePayload() throws Exception {
        // Missing required fields should trigger 400
        mvc.perform(post("/api/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsLogEntry() throws Exception {
        LogEntry created = new LogEntry();
        created.setId(10L);
        created.setTimestamp(Instant.parse("2024-01-01T00:00:00Z"));
        created.setLogLevel(LogLevel.ERROR);
        created.setSource("api");
        created.setMessage("boom");
        

        Mockito.when(service.create(any(LogEntry.class))).thenReturn(created);

        String json = "{\n" +
                "  \"timestamp\": \"2024-01-01T00:00:00Z\",\n" +
                "  \"logLevel\": \"ERROR\",\n" +
                "  \"source\": \"api\",\n" +
                "  \"message\": \"boom\"\n" +
                "}";

        mvc.perform(post("/api/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"id\":10")));
    }
}
