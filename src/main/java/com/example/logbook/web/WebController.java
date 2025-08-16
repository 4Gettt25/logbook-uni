package com.example.logbook.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/logs")
    public String logs() {
        return "logs";
    }

    @GetMapping("/create")
    public String create() {
        return "create";
    }

    @GetMapping("/servers")
    public String servers() {
        return "servers";
    }

    @GetMapping("/upload")
    public String upload() {
        return "upload";
    }
}
