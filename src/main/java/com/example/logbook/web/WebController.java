package com.example.logbook.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.thymeleaf.TemplateEngine;

public class WebController {
    
    private final TemplateEngine templateEngine;
    
    public WebController(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }
    
    public void registerRoutes(Javalin app) {
        app.get("/", this::index);
        app.get("/logs", this::logs);
        app.get("/create", this::create);
        app.get("/servers", this::servers);
        app.get("/upload", this::upload);
    }
      private void index(Context ctx) {
        org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
        String html = templateEngine.process("index", context);
        ctx.html(html);
    }
    
    private void logs(Context ctx) {
        org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
        String html = templateEngine.process("logs", context);
        ctx.html(html);
    }
    
    private void create(Context ctx) {
        org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
        String html = templateEngine.process("create", context);
        ctx.html(html);
    }
    
    private void servers(Context ctx) {
        org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
        String html = templateEngine.process("servers", context);
        ctx.html(html);
    }
    
    private void upload(Context ctx) {
        org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
        String html = templateEngine.process("upload", context);
        ctx.html(html);
    }
}
