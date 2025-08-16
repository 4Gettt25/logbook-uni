package com.example.logbook.web;

import com.example.logbook.domain.Server;
import com.example.logbook.repository.ServerRepository;
import com.example.logbook.service.LogImportService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { ServerController.class, RestExceptionHandler.class })
class ServerUploadControllerTests {

    @Autowired
    MockMvc mvc;

    @MockBean
    ServerRepository servers;

    @MockBean
    LogImportService imports;

    @Test
    void uploadsSingleFile() throws Exception {
        Server s = new Server();
        s.setId(1L);
        s.setName("server-a");
        Mockito.when(servers.findById(1L)).thenReturn(Optional.of(s));
        Mockito.when(imports.importText(any(byte[].class), any(Server.class))).thenReturn(3);

        MockMultipartFile file = new MockMultipartFile("file", "a.log", MediaType.TEXT_PLAIN_VALUE,
                ("2024-01-01T00:00:00Z INFO app - one\n" +
                        "2024-01-01T00:00:01Z INFO app - two\n" +
                        "2024-01-01T00:00:02Z INFO app - three\n").getBytes());

        mvc.perform(multipart("/api/servers/1/logs/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"total\":3")))
                .andExpect(content().string(containsString("\"file\":\"a.log\"")));
    }

    @Test
    void uploadsMultipleFiles() throws Exception {
        Server s = new Server();
        s.setId(1L);
        s.setName("server-a");
        Mockito.when(servers.findById(1L)).thenReturn(Optional.of(s));
        // return different counts per call
        Mockito.when(imports.importText(any(byte[].class), any(Server.class)))
                .thenReturn(2)
                .thenReturn(5);

        MockMultipartFile f1 = new MockMultipartFile("files", "a.log", MediaType.TEXT_PLAIN_VALUE, "line1\nline2\n".getBytes());
        MockMultipartFile f2 = new MockMultipartFile("files", "b.log", MediaType.TEXT_PLAIN_VALUE, "l1\nl2\nl3\nl4\nl5\n".getBytes());

        mvc.perform(multipart("/api/servers/1/logs/upload").file(f1).file(f2))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"total\":7")))
                .andExpect(content().string(containsString("\"file\":\"a.log\"")))
                .andExpect(content().string(containsString("\"file\":\"b.log\"")));
    }
}

