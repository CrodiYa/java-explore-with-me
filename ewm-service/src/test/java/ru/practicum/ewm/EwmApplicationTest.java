package ru.practicum.ewm;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {"logging.level.root=ERROR", "spring.main.banner-mode=off"})
public class EwmApplicationTest {

    @Test
    void contextLoads() {
        EwmApplication.main(new String[]{});
    }

}
