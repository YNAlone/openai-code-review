package plus.gaga.middleware.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 单元测试示例类
 * <p>
 * 基于 JUnit4 + Spring Boot Test 框架，用于验证基础功能或快速排查问题。
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ApiTest {

    /**
     * 简单测试用例，验证 Inteter.parseInt 对非法字符串会抛出 NumberFormatException
     */
    @Test
    public void test() {
        // 传入非数字字符串，预期抛出 NumberFormatException
        System.out.println(Integer.parseInt("aa33321"));
//        System.out.println(Integer.parseInt("aaaa2"));
//        System.out.println(Integer.parseInt("aaaa3"));
    }

}
