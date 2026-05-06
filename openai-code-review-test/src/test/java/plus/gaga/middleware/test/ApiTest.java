package plus.gaga.middleware.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ApiTest {

    @Test
    public void test() {
        System.out.println(Integer.parseInt("z1235a231"));
        System.out.println(Integer.parseInt("z123213124a231"));
        System.out.println(Integer.parseInt("bb125422"));
        System.out.println(Integer.parseInt("cc33"));
    }

}
