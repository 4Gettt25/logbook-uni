package com.example.logbook.config;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class ThymeleafConfig {
    
    private final TemplateEngine templateEngine;
    
    public ThymeleafConfig() {
        this.templateEngine = createTemplateEngine();
    }
    
    private TemplateEngine createTemplateEngine() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setPrefix("/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setCacheable(false); // Set to true in production
        templateResolver.setCharacterEncoding("UTF-8");
        
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(templateResolver);
        
        return engine;
    }
    
    public TemplateEngine getTemplateEngine() {
        return templateEngine;
    }
}
