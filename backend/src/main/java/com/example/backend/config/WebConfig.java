package com.example.backend.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * The frontend jar puts its build output in classpath:/static/, which Spring Boot serves
 * automatically, including index.html at "/". This only adds the client-side route fallback.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
    // Replaces Spring Boot's default "/**" static handler, so it keeps the same location
    registry
        .addResourceHandler("/**")
        .addResourceLocations("classpath:/static/")
        .resourceChain(true)
        .addResolver(new SpaFallbackResourceResolver());
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
