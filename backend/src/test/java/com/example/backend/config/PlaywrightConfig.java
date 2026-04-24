package com.example.backend.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import jakarta.annotation.PreDestroy;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

@Slf4j
@TestConfiguration(proxyBeanMethods = false)
public class PlaywrightConfig {
  private static Playwright playwrightInstance;
  private static Browser browserInstance;

  @Bean
  public synchronized Playwright playwright() {
    if (playwrightInstance == null) {
      playwrightInstance = Playwright.create();
    }
    return playwrightInstance;
  }

  @Bean
  public synchronized Browser browser(Playwright playwright) {
    if (browserInstance == null) {
      browserInstance =
          playwright
              .chromium()
              .launch(
                  new BrowserType.LaunchOptions()
                      .setHeadless(true)
                      .setArgs(Arrays.asList("--disable-sandbox", "--disable-dev-shm-usage")));
    }
    return browserInstance;
  }

  @Bean
  @Scope("prototype")
  public BrowserContext browserContext(Browser browser) {
    return browser.newContext(
        new Browser.NewContextOptions().setViewportSize(1920, 1080));
  }

  @Bean
  @Scope("prototype")
  public Page page(BrowserContext browserContext) {
    return browserContext.newPage();
  }

  @PreDestroy
  public void cleanup() {
    log.info("Cleaning up Playwright resources");
    if (browserInstance != null) {
      try {
        browserInstance.close();
      } catch (Exception e) {
        log.error("Error closing browser", e);
      }
      browserInstance = null;
    }
    if (playwrightInstance != null) {
      try {
        playwrightInstance.close();
      } catch (Exception e) {
        log.error("Error closing playwright", e);
      }
      playwrightInstance = null;
    }
  }
}
