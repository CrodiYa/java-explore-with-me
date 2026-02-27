package ru.practicum.stats.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {"logging.level.root=ERROR", "spring.main.banner-mode=off"})
class ViewStatsServerAppTest {
    @Test
    void contextLoads() {
        ViewStatsServerApp.main(new String[]{});
    }
}