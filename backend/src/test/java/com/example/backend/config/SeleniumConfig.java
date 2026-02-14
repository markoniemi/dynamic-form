package com.example.backend.config;

import io.github.bonigarcia.wdm.WebDriverManager;
import java.time.Duration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.context.annotation.Bean;

public class SeleniumConfig {
  @Bean
  public WebDriver getWebDriver() {
    WebDriverManager.chromedriver().setup();
    ChromeOptions options = new ChromeOptions();
    options.addArguments("headless=new");
    options.addArguments("window-size=1920,1080");
    options.addArguments("no-sandbox");
    options.addArguments("proxy-server='direct://'");
    options.addArguments("proxy-bypass-list=*");
    options.addArguments("remote-allow-origins=*");
    options.addArguments("guest");
    ChromeDriver chromeDriver = new ChromeDriver(options);
    chromeDriver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));
    return chromeDriver;
  }
}
