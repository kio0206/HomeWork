import org.junit.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;

public class ChaoxingAutomationTest {

    private WebDriver driver;
    private WebDriverWait wait;

    @Before
    public void setUp() {
        System.setProperty("webdriver.chrome.driver", "D:\\tools\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    /**D:\tools\chromedriver-win64\chromedriver-win64\chromedriver.exe
     * D:\tools\chromedriver-win64\chromedriver-win64\chromedriver.exe
     * 1. 登录测试
     */
    @Test
    public void testLogin() {
        driver.get("D:\\tools\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");

        WebElement usernameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("phone")));
        usernameInput.sendKeys("你的账号"); // 替换

        WebElement passwordInput = driver.findElement(By.id("pwd"));
        passwordInput.sendKeys("你的密码"); // 替换

        WebElement loginBtn = driver.findElement(By.id("loginBtn"));
        loginBtn.click();

        wait.until(ExpectedConditions.urlContains("chaoxing.com"));
        String url = driver.getCurrentUrl();
        Assert.assertTrue("未成功登录", url.contains("chaoxing.com"));
    }

    /**
     * 2. 发起讨论测试（需先登录成功）
     */
    @Test
    public void testCreateTopic() throws InterruptedException {
        login(); // 调用登录方法

        // 模拟跳转到某课程的讨论区（页面结构可能变化，请根据实际定位）
        driver.get("https://mooc1.chaoxing.com/mycourse/studentstudy?chapterId=xxx&courseId=xxx"); // 修改为你自己的课程ID

        // 假设点击进入“话题讨论”标签
        WebElement discussTab = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("话题")));
        discussTab.click();

        // 点击“发起话题”
        WebElement newTopic = wait.until(ExpectedConditions.elementToBeClickable(By.id("newTopicBtn")));
        newTopic.click();

        // 填写话题标题与内容
        driver.findElement(By.name("title")).sendKeys("测试话题标题");
        driver.findElement(By.name("content")).sendKeys("这是通过Selenium自动发布的话题内容。");

        driver.findElement(By.id("submitTopic")).click();

        // 验证发布是否成功（这里用关键词断言）
        Thread.sleep(2000); // 等待刷新
        Assert.assertTrue(driver.getPageSource().contains("测试话题标题"));
    }

    /**
     * 3. 回复讨论测试（需登录和已有话题）
     */
    @Test
    public void testReplyTopic() throws InterruptedException {
        login();

        // 跳转到讨论详情页（你需要替换具体URL）
        driver.get("https://mooc1.chaoxing.com/bbs/app/topic?courseId=xxx&topicId=xxx"); // 修改为你自己的话题链接

        // 输入并提交回复
        WebElement replyBox = wait.until(ExpectedConditions.elementToBeClickable(By.name("replyContent")));
        replyBox.sendKeys("这是自动化测试的回复内容");

        WebElement replyBtn = driver.findElement(By.id("replySubmit"));
        replyBtn.click();

        Thread.sleep(2000); // 等待页面刷新
        Assert.assertTrue(driver.getPageSource().contains("这是自动化测试的回复内容"));
    }

    /**
     * 公共登录方法供其他测试复用
     */
    private void login() {
        driver.get("https://passport2.chaoxing.com/login?fid=&newversion=true&refer=https%3A%2F%2Fi.chaoxing.com");

        WebElement usernameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("phone")));
        usernameInput.sendKeys("你的账号");

        WebElement passwordInput = driver.findElement(By.id("pwd"));
        passwordInput.sendKeys("你的密码");

        WebElement loginBtn = driver.findElement(By.id("loginBtn"));
        loginBtn.click();

        wait.until(ExpectedConditions.urlContains("chaoxing.com"));
    }

    @After
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
