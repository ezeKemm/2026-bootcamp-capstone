package com.northstar.crm.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Opens a fresh Chrome for every test and closes it afterwards.
 * Needs the app running: Docker (database), backend on :8080, frontend on :4200.
 *   -De2e.baseUrl=...   point at another frontend (default http://localhost:4200)
 *   -De2e.headless=true run without a visible window (for CI later)
 */
abstract class BaseE2ETest {

    protected static final String BASE_URL =
            System.getProperty("e2e.baseUrl", "http://localhost:4200");

    protected WebDriver driver;
    protected WebDriverWait wait;

    @BeforeEach
    void openBrowser() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1280,900");
        if (Boolean.parseBoolean(System.getProperty("e2e.headless", "false"))) {
            options.addArguments("--headless=new");
        }
        driver = new ChromeDriver(options);
        // Angular loads data after the page appears, so always wait for elements instead of reading them right away.
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }
}
