package com.northstar.crm.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Browser end-to-end tests (Selenium). Opens a fresh Chrome for every test and closes it afterwards.
 *
 * Tagged "e2e" and skipped by a normal `mvnw test` / CI (see excludedGroups in pom.xml),
 * because they need Chrome and the whole app running: Docker (database), backend on :8080, frontend on :4200.
 *
 * Run them on purpose from backend/:
 *   .\mvnw test -Pe2e
 * Options:
 *   -De2e.baseUrl=...   point at another frontend (default http://localhost:4200)
 *   -De2e.headless=true run without a visible window (for CI later)
 */
@Tag("e2e")
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

    /** Helper function to select on `data-testid` HTML attribute.
     *  Should always select on data-testid over more fragile selectors which can change, unless necessary.
     *  */
    public static By byTestId(String testId) {
        return By.cssSelector("[data-testid='" + testId + "'");
    }
}
