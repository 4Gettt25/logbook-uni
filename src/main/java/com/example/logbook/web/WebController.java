package com.example.logbook.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import jakarta.servlet.ServletContext;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

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
        WebContext context = createContext(ctx);
        String html = templateEngine.process("index", context);
        ctx.html(html);
    }

    private void logs(Context ctx) {
        WebContext context = createContext(ctx);
        String html = templateEngine.process("logs", context);
        ctx.html(html);
    }

    private void create(Context ctx) {
        WebContext context = createContext(ctx);
        String html = templateEngine.process("create", context);
        ctx.html(html);
    }

    private void servers(Context ctx) {
        WebContext context = createContext(ctx);
        String html = templateEngine.process("servers", context);
        ctx.html(html);
    }

    private void upload(Context ctx) {
        WebContext context = createContext(ctx);
        String html = templateEngine.process("upload", context);
        ctx.html(html);
    }

    private WebContext createContext(Context ctx) {
        ServletContext servletContext = ctx.req().getServletContext();
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
        IWebExchange exchange = application.buildExchange(ctx.req(), ctx.res());
        WebContext webContext = new WebContext(exchange);
        webContext.setVariable("requestPath", ctx.path());
        return webContext;
    }
}
