import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.*;

public class ImprovedIndexBuilder implements AutoCloseable {

    private IndexWriter writer;
    private Analyzer analyzer;

    // 构造方法，用于初始化索引写入器
    public ImprovedIndexBuilder(String indexDir) throws IOException {
        // 打开或创建索引目录
        Directory dir = FSDirectory.open(Paths.get(indexDir));
        // 使用中文分词器
        analyzer = new SmartChineseAnalyzer();
        // 配置索引写入器
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        // 设置索引打开模式为创建或追加
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        // 初始化索引写入器
        writer = new IndexWriter(dir, iwc);
    }

    // 索引文档的方法
    public void indexDocument(String id, String url, String title, String description, String keywords, String detail, String content, String time) throws IOException {
        // 创建文档对象
        Document doc = new Document();
        // 添加字段到文档，这里使用了不同类型的字段
        doc.add(new StringField("id", id, Field.Store.YES));
        doc.add(new StringField("url", url, Field.Store.YES));
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new TextField("description", description, Field.Store.YES));
        doc.add(new TextField("keywords", keywords, Field.Store.YES));
        doc.add(new TextField("detail", detail, Field.Store.YES));
        doc.add(new TextField("content", content, Field.Store.YES));
        doc.add(new StringField("time", time, Field.Store.YES)); // 时间作为字符串处理

        // 将文档添加到索引
        writer.addDocument(doc);
    }

    // 实现 AutoCloseable 接口，以便在 try-with-resources 语句中自动关闭资源
    @Override
    public void close() throws IOException {
        // 关闭索引写入器
        writer.close();
    }

    public static void main(String[] args) {
        // 指定索引目录
        String indexDir = "/Users/fujunqiao/Documents/code/java/SearchEngine/Index";
        // 数据库连接信息
        String jdbcUrl = "jdbc:mysql://localhost:3306/topic-search";
        String user = "root";
        String password = "mysql088925";
        // SQL 查询语句
        String sqlQuery = "SELECT * FROM crawled_url_rank";

        try {
            // 加载数据库驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            // 创建索引构建器，连接数据库，执行查询
            try (ImprovedIndexBuilder builder = new ImprovedIndexBuilder(indexDir);
                 Connection conn = DriverManager.getConnection(jdbcUrl, user, password);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlQuery)) {

                // 遍历查询结果并构建索引
                while (rs.next()) {
                    String id = rs.getString("id");
                    String url = rs.getString("url");
                    String title = rs.getString("title");
                    String content = rs.getString("content");
                    String keywords = rs.getString("keywords");
                    String detail = rs.getString("detail");
                    // 提取摘要
                    String description = summarizeContent(content);
                    String time = rs.getString("time"); // 直接获取时间字符串

                    // 添加文档到索引
                    builder.indexDocument(id, url, title, description, keywords, detail, content, time);
                }

                // 索引构建完成
                System.out.println("索引构建完成.");
            }
        } catch (ClassNotFoundException | SQLException | IOException e) {
            // 错误处理
            System.out.println("发生错误.");
            e.printStackTrace();
        }
    }

    // 生成内容摘要的方法
    private static String summarizeContent(String content) {
        // 如果内容超过100个字符，则截取前100个字符并添加省略号
        return content.length() > 100 ? content.substring(0, 100) + "..." : content;
    }
}
