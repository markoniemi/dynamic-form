package com.example.backend.e2e.pages;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.WebDriverWait;

@Slf4j
public abstract class BasePage {
  protected WebDriver driver;
  protected WebDriverWait wait;

  public BasePage(WebDriver driver) {
    this.driver = driver;
    this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    PageFactory.initElements(driver, this);
  }

  protected void sleep(int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      log.warn(e.getMessage(), e);
    }
  }
}
