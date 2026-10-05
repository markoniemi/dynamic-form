package com.example.backend.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/**")
        .addResourceLocations("classpath:/META-INF/resources/webjars/frontend/1.0.0-SNAPSHOT/")
        .resourceChain(true)
        .addResolver(new SpaFallbackResourceResolver());
  }

  @Override
  public void addViewControllers(@NonNull ViewControllerRegistry registry) {
    registry.addViewController("/").setViewName("forward:/index.html");
  }

  /**
   * Serves index.html for client-side routes (e.g. /login, /forms/contact) so that reloading
   * the page lets React Router handle the path. API paths and missing files keep returning 404.
   */
  private static class SpaFallbackResourceResolver extends PathResourceResolver {
    @Override
    protected Resource getResource(@NonNull String resourcePath, @NonNull Resource location)
        throws IOException {
      Resource resource = super.getResource(resourcePath, location);
      if (resource != null || resourcePath.startsWith("api/") || resourcePath.contains(".")) {
        return resource;
      }
      return super.getResource("index.html", location);
    }
  }
}
