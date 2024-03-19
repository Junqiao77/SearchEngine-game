import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class WebCrawler {

    // 用于存储待爬取的URL队列
    private Queue<String> queue = new LinkedList<>();

    // 用于存储已经处理过的URL集合，避免重复爬取
    private Set<String> seenUrls = new HashSet<>();

    // 最大URL数量限制（自行修改MAX_URLS的值达到合适的网页数量）
    private static final int MAX_URLS = 70000;  

    // 数据库连接URL，用户名和密码
    private static final String DB_URL = "jdbc:mysql://localhost/topic-search?user=你的用户名&password=你的密码";

    // SQL插入语句（插入你想要的字段）
    private static final String INSERT_SQL = "INSERT INTO crawled_url_rank (url, title, description, keywords, detail, content, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?);";

    // 用户代理信息，模拟浏览器
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3";

    // 爬取延迟，以毫秒为单位（设置合适的爬取延迟，对于比较大的网站建议设置较长的延迟）
    private static final int CRAWL_DELAY_MS = 500;

    // Cookie信息，可以用于模拟登录状态
    private static final String COOKIE = " 这里是你的cookie "
    
    // 构造函数，接受种子URL作为参数，并初始化队列和已处理URL集合
    public WebCrawler(String seedUrl) {
        if (isValidUrl(seedUrl)) {
            queue.offer(seedUrl);
            seenUrls.add(seedUrl);
        }
    }

    // 验证URL是否有效
    private static boolean isValidUrl(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 判断是否应该爬取特定的URL，这里使用正则表达式匹配URL（根据需要修改）
    private boolean shouldCrawlUrl(String url) {
        return url.matches("https://www.gamersky.com/z/.*/") ||
                url.matches("https://www.gamersky.com/z/.*/news/") ||
                url.matches("https://www.gamersky.com/z/.*/handbook/") ||
                url.matches("https://www.gamersky.com/news/.*") ||
                url.matches("https://www.gamersky.com/handbook/.*");
    }

    // 建立数据库连接
    private Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    // 插入数据到数据库
    private void insertData(String url, String title, String description, String keywords, String detail, String content, long timestamp) {
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {
            pstmt.setString(1, url);
            pstmt.setString(2, title);
            pstmt.setString(3, description);
            pstmt.setString(4, keywords);
            pstmt.setString(5, detail);
            pstmt.setString(6, content);
            pstmt.setLong(7, timestamp);
            pstmt.executeUpdate();
            System.out.println("已保存到数据库: " + url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // 开始爬取网页
    public void startCrawl() {
        int countUrls = 0;
        while (!queue.isEmpty() && countUrls < MAX_URLS) {
            String currentUrl = queue.poll();
            System.out.println("正在爬取URL: " + currentUrl);
            try {
                Document doc = Jsoup.connect(currentUrl).userAgent(USER_AGENT).cookie("Cookie", COOKIE).get();

                // 获取网页标题、描述和关键字
                String title = doc.title();
                String description = doc.select("meta[name=description]").attr("content");
                String keywords = doc.select("meta[name=keywords]").attr("content");

                // 获取网页详细信息和内容
                Element detailElement = doc.selectFirst(".Mid2L_tit .detail");
                String detail = detailElement != null ? detailElement.text() : "";

                Elements contentElements = doc.select(".Mid2L_con p, .Mid2L_con .GsImageLabel");
                StringBuilder contentBuilder = new StringBuilder();
                for (Element element : contentElements) {
                    contentBuilder.append(element.text()).append("\n");
                }
                String content = contentBuilder.toString().trim();
                long timestamp = System.currentTimeMillis();

                // 插入数据到数据库
                insertData(currentUrl, title, description, keywords, detail, content, timestamp);

                // 获取页面中的链接，并加入队列
                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    String absHref = link.attr("abs:href");
                    if (!seenUrls.contains(absHref) && isValidUrl(absHref) && shouldCrawlUrl(absHref)) {
                        seenUrls.add(absHref);
                        queue.offer(absHref);
                    }
                }

                countUrls++;
                Thread.sleep(CRAWL_DELAY_MS);
            } catch (Exception e) {
                System.err.println("处理URL时出错: " + currentUrl + "; 错误信息: " + e.getMessage());
            }
        }
    }

    // 主方法，程序入口
    public static void main(String[] args) {
        WebCrawler crawler = new WebCrawler("https://www.gamersky.com/");
        crawler.startCrawl();
    }
}
