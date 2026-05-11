package plus.gaga.middleware.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ApiTest {

//    @Test
//    public void test() {
//        System.out.println(Integer.parseInt("z15164"));
//        System.out.println(Integer.parseInt("z123213124a231"));
//        System.out.println(Integer.parseInt("bb125422"));
//        System.out.println(Integer.parseInt("cc33"));
//    }
    // ====================== 1. 空指针异常类 ======================
/*    @Test
    public void testNullPointerException_DirectCall() {
        // 直接调用null对象的方法
        String str = null;
        System.out.println(str.length());
    }

    @Test
    public void testNullPointerException_CollectionAccess() {
        // 集合为null时调用方法
        List<String> list = null;
        list.add("test");
    }

    @Test
    public void testNullPointerException_MapGet() {
        // Map中不存在的key返回null后直接调用方法
        Map<String, String> map = new HashMap<>();
        String value = map.get("non_exist_key");
        System.out.println(value.trim());
    }

    @Test
    public void testNullPointerException_ParameterCheckMissing() {
        // 方法参数未做空值检查
        processString(null);
    }

    private void processString(String str) {
        // 缺少null检查
        System.out.println(str.toUpperCase());
    }*/

    // ====================== 2. SQL注入风险类 ======================
  /*  @Test
    public void testSQLInjection_StringConcatenation() {
        // 直接拼接用户输入到SQL语句
        String username = "admin' OR '1'='1";
        String sql = "SELECT * FROM users WHERE username = '" + username + "'";
        executeSQL(sql);
    }

    @Test
    public void testSQLInjection_StatementUsage() {
        // 使用Statement而非PreparedStatement
        String userInput = "test'; DROP TABLE users; --";
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/test", "root", "123456");
            Statement stmt = conn.createStatement();
            stmt.execute("INSERT INTO logs (content) VALUES ('" + userInput + "')");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSQLInjection_LikeClause() {
        // LIKE子句中的SQL注入
        String keyword = "%' OR 1=1 --";
        String sql = "SELECT * FROM products WHERE name LIKE '%" + keyword + "%'";
        executeSQL(sql);
    }

    private void executeSQL(String sql) {
        // 模拟SQL执行
        System.out.println("执行SQL: " + sql);
    }*/

  /*  // ====================== 3. 命名不规范类 ======================
    @Test
    public void testNamingConvention_ClassName() {
        // 类名使用小写开头（此处为测试用例，实际类名已规范）
        class userService {
            public void addUser() {}
        }
    }

    @Test
    public void testNamingConvention_MethodName() {
        // 方法名使用大驼峰
        String UserName = getUserName();
    }

    @Test
    public void testNamingConvention_ConstantName() {
        // 常量未使用全大写
        final int maxRetryCount = 3;
        System.out.println(maxRetryCount);
    }

    @Test
    public void testNamingConvention_MeaninglessName() {
        // 无意义的变量名
        int a = 100;
        String b = "test";
        System.out.println(a + b);
    }

    private String getUserName() {
        return "test_user";
    }*/

/*    // ====================== 4. 异常处理缺失类 ======================
    @Test
    public void testExceptionHandling_EmptyCatch() {
        // 空的catch块，吞掉异常
        try {
            int result = 10 / 0;
        } catch (Exception e) {
            // 什么都不做
        }
    }

    @Test
    public void testExceptionHandling_PrintStackTraceOnly() {
        // 仅打印堆栈，不做任何处理
        try {
            FileInputStream fis = new FileInputStream("non_exist_file.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testExceptionHandling_CatchThrowable() {
        // 捕获过于宽泛的Throwable
        try {
            // 业务代码
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }*/

    // ====================== 5. 资源未关闭类 ======================
    @Test
    public void testResourceLeak_FileInputStream() {
        // 文件流未关闭
        try {
            FileInputStream fis = new FileInputStream("test.txt");
            byte[] data = new byte[1024];
            fis.read(data);
            // 缺少fis.close()
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testResourceLeak_Connection() {
        // 数据库连接未关闭
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/test", "root", "123456");
            Statement stmt = conn.createStatement();
            stmt.execute("SELECT * FROM users");
            // 缺少conn.close()和stmt.close()
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    // ====================== 6. 低效代码/性能问题类 ======================
//    @Test
//    public void testPerformance_StringConcatenationInLoop() {
//        // 循环中使用+拼接字符串
//        String result = "";
//        for (int i = 0; i < 1000; i++) {
//            result += i;
//        }
//        System.out.println(result);
//    }
//
//    @Test
//    public void testPerformance_UnnecessaryObjectCreation() {
//        // 循环中创建不必要的对象
//        for (int i = 0; i < 1000; i++) {
//            Integer num = new Integer(i); // 应该使用Integer.valueOf(i)
//            System.out.println(num);
//        }
//    }
//
//    @Test
//    public void testPerformance_ArrayListInitialCapacity() {
//        // 未指定ArrayList初始容量，导致频繁扩容
//        List<String> list = new ArrayList<>();
//        for (int i = 0; i < 10000; i++) {
//            list.add("item" + i);
//        }
//    }
//
//    // ====================== 7. 安全隐患类 ======================
//    @Test
//    public void testSecurity_HardcodedCredentials() {
//        // 敏感信息硬编码
//        String dbPassword = "123456";
//        String apiKey = "sk_abcdefghijklmnopqrstuvwxyz";
//        System.out.println("连接数据库，密码: " + dbPassword);
//    }
//
//    @Test
//    public void testSecurity_HardcodedIP() {
//        // IP地址硬编码
//        String serverUrl = "http://192.168.1.100:8080/api";
//        System.out.println("访问服务器: " + serverUrl);
//    }
//
//    // ====================== 8. 代码规范问题类 ======================
//    @Test
//    public void testCodeStyle_MagicValue() {
//        // 魔法值未定义为常量
//        if (user.getAge() > 18) {
//            System.out.println("成年人");
//        }
//    }
//
//    @Test
//    public void testCodeStyle_UnusedImport() {
//        // 无用的导入（本文件顶部导入了java.util.ArrayList但未使用）
//        System.out.println("测试无用导入");
//    }
//
//    @Test
//    public void testCodeStyle_LongMethod() {
//        // 方法过长（超过50行）
//        // 此处省略50行以上的代码
//        System.out.println("这是一个过长的方法");
//    }
//
//    // 模拟用户类
//    static class User {
//        private int age;
//        public int getAge() { return age; }
//    }
//
//    private User user = new User();
}

