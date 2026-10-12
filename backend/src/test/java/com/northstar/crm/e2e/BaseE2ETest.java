package com.northstar.crm.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        // Stop Chrome's own password popups ("Save password?", "Change your password" breach warning).
        // They sit on top of the page and block clicks after logging in with the weak demo accounts (agent1/agent1).
        // TODO(e2e): Remove these password-manager settings once the demo accounts are replaced with real users
        //  and strong passwords, so Chrome no longer flags them as breached.
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.password_manager_leak_detection", false);
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--disable-features=PasswordLeakDetection");
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
        return By.cssSelector("[data-testid='" + testId + "']");
    }
    /**
     * Fills in the login form and submits it.
     * Call it while on the login page (e.g. after opening a protected page as a guest).
     */
    protected void signIn(String username, String password) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username"))).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.cssSelector("form button[type='submit']")).click();
    }

    /** Clicks a customer's row on the signed-in /customers list, by full name. */
    protected void openCustomer(String fullName) {
        List<WebElement> rows = wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.cssSelector("table.customers tr.clickable-row"), 0));
        rows.stream()
                .filter(row -> row.findElement(By.cssSelector(".name-cell")).getText().equals(fullName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No row for " + fullName))
                .click();
    }
}
