package com.example.logbook.config;

import org.thymeleaf.context.Context;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.IWebRequest;
import org.thymeleaf.web.IWebSession;

import java.util.Locale;
import java.util.Map;

public class WebContext extends Context implements IWebContext {
    
    private final IWebExchange webExchange;
    private final String contextPath;
    
    public WebContext(IWebExchange webExchange, Locale locale, Map<String, Object> variables) {
        super(locale, variables);
        this.webExchange = webExchange;
        this.contextPath = "/"; // Default context path for standalone app
    }
    
    public WebContext(Locale locale, Map<String, Object> variables) {
        super(locale, variables);
        this.webExchange = null;
        this.contextPath = "/";
    }
    
    @Override
    public IWebExchange getExchange() {
        return webExchange;
    }
    
    @Override
    public IWebRequest getRequest() {
        return webExchange != null ? webExchange.getRequest() : null;
    }
    
    @Override
    public IWebSession getSession() {
        return webExchange != null ? webExchange.getSession() : null;
    }
    
    // For compatibility, provide a simple context path method
    public String getContextPath() {
        return contextPath;
    }
}
