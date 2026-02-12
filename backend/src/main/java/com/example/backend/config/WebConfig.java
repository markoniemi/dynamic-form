package com.example.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(
      @org.springframework.lang.NonNull ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/**")
        .addResourceLocations("classpath:/META-INF/resources/webjars/frontend/1.0.0/");
  }

  @Override
  public void addViewControllers(
      @org.springframework.lang.NonNull ViewControllerRegistry registry) {
    registry.addViewController("/").setViewName("forward:/index.html");
  }
}
