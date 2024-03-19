import javax.swing.*;  // 导入Swing库，用于图形界面设计
import java.awt.*;  // 导入AWT库，用于更多的界面元素
import java.awt.event.ActionEvent;  // 导入事件处理
import java.awt.event.ActionListener;  // 导入事件监听
import java.io.IOException;  // 导入异常处理
import org.apache.lucene.queryparser.classic.ParseException;  // 导入Lucene库异常处理
import org.apache.lucene.search.TopDocs;  // 导入Lucene的搜索结果处理
import org.apache.lucene.search.ScoreDoc;  // 导入Lucene评分文档处理
import org.apache.lucene.document.Document;  // 导入Lucene文档处理

// 创建一个名为SearchEngineGUI的类，继承自JFrame，用于创建图形界面
public class SearchEngineGUI extends JFrame {
    // 定义界面上的各种组件
    private JTextField searchTextField, advancedSearchTextField;  // 搜索框和高级搜索框
    private JTextArea resultArea;  // 结果展示区域
    private JButton searchButton;  // 搜索按钮
    private JRadioButton andButton, orButton, notButton;  // 逻辑运算符按钮
    private JButton nextPageButton, prevPageButton;  // 分页按钮
    private JLabel pageInfo;  // 页面信息标签
    private int currentPage = 1;  // 当前页码
    private static final int PAGE_SIZE = 10;  // 每页显示的记录数
    private SearchEngine searchEngine;  // 搜索引擎对象

    // 类的构造函数，用于初始化
    public SearchEngineGUI() throws IOException {
        // 初始化搜索引擎，指定索引位置
        searchEngine = new SearchEngine("/Users/fujunqiao/Documents/code/java/Games-search/Index");

        createView();  // 创建视图

        // 为搜索按钮添加事件监听器
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    performSearch();  // 执行搜索
                } catch (Exception ex) {
                    ex.printStackTrace();
                    resultArea.setText("搜索时发生错误: " + ex.getMessage());  // 错误处理
                }
            }
        });

        // 为下一页按钮添加事件监听器
        nextPageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentPage++;  // 页码增加
                try {
                    performSearch();  // 执行搜索
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // 为上一页按钮添加事件监听器
        prevPageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentPage > 1) {
                    currentPage--;  // 页码减少
                    try {
                        performSearch();  // 执行搜索
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        // 设置窗体标题、关闭操作和布局
        setTitle("游戏搜索引擎");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    // 创建界面视图的方法
    private void createView() {
        JPanel panel = new JPanel(new BorderLayout());  // 使用边界布局
        getContentPane().add(panel);


        // 创建顶部面板用于放置基础搜索框和高级搜索框
        JPanel topPanel = new JPanel(new BorderLayout());

        searchTextField = new JTextField();  // 创建基础搜索框
        topPanel.add(searchTextField, BorderLayout.NORTH);  // 将基础搜索框添加到顶部面板的北部

        // 创建包含逻辑运算符按钮和高级搜索框的面板
        JPanel advancedSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // 创建逻辑运算符按钮并添加到高级搜索面板
        andButton = new JRadioButton("AND");
        orButton = new JRadioButton("OR");
        notButton = new JRadioButton("NOT");
        ButtonGroup group = new ButtonGroup();
        group.add(andButton);
        group.add(orButton);
        group.add(notButton);
        advancedSearchPanel.add(andButton);
        advancedSearchPanel.add(orButton);
        advancedSearchPanel.add(notButton);

        advancedSearchTextField = new JTextField(40);  // 创建高级搜索框
        advancedSearchPanel.add(advancedSearchTextField);  // 将高级搜索框添加到高级搜索面板
        topPanel.add(advancedSearchPanel, BorderLayout.SOUTH);  // 将高级搜索面板添加到顶部面板的南部

        panel.add(topPanel, BorderLayout.NORTH);  // 将顶部面板添加到主面板的北部

        resultArea = new JTextArea();  // 创建结果展示区域
        resultArea.setEditable(false);  // 设置为不可编辑
        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);  // 添加滚动条并放置于中央

        // 创建南部面板，只包含分页按钮和搜索按钮
        JPanel southPanel = new JPanel(new FlowLayout());  // 使用流布局

        prevPageButton = new JButton("上一页");  // 创建上一页按钮
        nextPageButton = new JButton("下一页");  // 创建下一页按钮
        southPanel.add(prevPageButton);  // 添加上一页按钮到南部面板
        southPanel.add(nextPageButton);  // 添加下一页按钮到南部面板

        searchButton = new JButton("Search");  // 创建搜索按钮
        southPanel.add(searchButton);  // 添加搜索按钮到南部面板

        pageInfo = new JLabel("Page: ");  // 创建页面信息标签并初始化
        southPanel.add(pageInfo);  // 将页面信息标签添加到南部面板

        panel.add(southPanel, BorderLayout.SOUTH);  // 将南部面板添加到主面板的南部
    }




    // 执行搜索的方法
    private void performSearch() throws IOException, ParseException {
        String query = searchTextField.getText();  // 获取搜索框文本
        String advancedQuery = advancedSearchTextField.getText();  // 获取高级搜索框文本
        String searchType = andButton.isSelected() ? "AND" : orButton.isSelected() ? "OR" : "NOT";  // 获取选择的逻辑运算符
        TopDocs results;  // 用于存储搜索结果

        // 根据不同的逻辑运算符执行不同的搜索
        if (searchType.equals("AND") || searchType.equals("OR") || searchType.equals("NOT")) {
            advancedSearchTextField.setVisible(true);  // 显示高级搜索框
            if (!advancedQuery.isEmpty()) {
                if (searchType.equals("AND")) {
                    // 执行AND搜索
                    results = searchEngine.advancedSearch(new String[]{query, advancedQuery}, new String[0], new String[0], currentPage, PAGE_SIZE);
                } else if (searchType.equals("OR")) {
                    // 执行OR搜索
                    results = searchEngine.advancedSearch(new String[0], new String[]{query, advancedQuery}, new String[0], currentPage, PAGE_SIZE);
                } else { // NOT
                    // 执行NOT搜索
                    results = searchEngine.advancedSearch(new String[]{query}, new String[0], new String[]{advancedQuery}, currentPage, PAGE_SIZE);
                }
            } else {
                results = searchEngine.advancedSearch(new String[]{query}, new String[0], new String[0], currentPage, PAGE_SIZE);  // 仅使用基础查询
            }
        } else {
            advancedSearchTextField.setVisible(false);  // 隐藏高级搜索框
            results = searchEngine.search(query, currentPage, PAGE_SIZE);  // 执行基本搜索
        }

        displayResults(results, currentPage, PAGE_SIZE);  // 展示搜索结果
    }

    // 展示搜索结果的方法
    private void displayResults(TopDocs hits, int page, int pageSize) throws IOException {
        resultArea.setText("");  // 清空结果区域
        for (ScoreDoc sd : hits.scoreDocs) {
            Document d = searchEngine.getSearcher().doc(sd.doc);  // 获取文档
            // 将文档的各个字段添加到结果区域
            resultArea.append("标题：" + d.get("title") + "\n");
            resultArea.append("简介：" + d.get("description") + "\n");
            resultArea.append("URL：" + d.get("url") + "\n");
            resultArea.append("详情：" + d.get("detail") + "\n");
            resultArea.append("时间：" + d.get("time") + "\n");
            resultArea.append("\n-------------------------------------------------\n");
        }

        int totalHits = (int) hits.totalHits.value;  // 获取总命中数
        int totalPages = (int) Math.ceil((double) totalHits / pageSize);  // 计算总页数
        pageInfo.setText("Page: " + page + " / " + totalPages);  // 更新页面信息标签
    }

    // 程序入口点
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    new SearchEngineGUI().setVisible(true);  // 创建并显示界面
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}

