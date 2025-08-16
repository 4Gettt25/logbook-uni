package com.example.logbook.web;

import com.example.logbook.domain.Server;
import com.example.logbook.repository.ServerRepository;
import com.example.logbook.service.LogEntryService;
import com.example.logbook.service.LogImportService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = { ServerController.class, RestExceptionHandler.class })
class ServerControllerCreateTests {

    @Autowired
    MockMvc mvc;

    @MockBean
    ServerRepository servers;

    @MockBean
    LogEntryService logService;

    @MockBean
    LogImportService importService;

    @Test
    void createsServerAndReturns201() throws Exception {
        Server saved = new Server();
        saved.setId(1L);
        saved.setName("web-1");
        saved.setHostname("host1");
        saved.setDescription("prod");
        Mockito.when(servers.saveAndFlush(any(Server.class))).thenReturn(saved);

        String json = "{\"name\":\"web-1\",\"hostname\":\"host1\",\"description\":\"prod\"}";

        mvc.perform(post("/api/servers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/servers/1")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"name\":\"web-1\"")));
    }
}

