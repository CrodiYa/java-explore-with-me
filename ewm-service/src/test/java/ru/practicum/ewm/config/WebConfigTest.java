package ru.practicum.ewm.config;

import org.junit.jupiter.api.Test;
import org.springframework.format.FormatterRegistry;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class WebConfigTest {

    @Test
    public void shouldAddStringToInstantConverter() {
        WebConfig webConfig = new WebConfig();
        FormatterRegistry registry = mock(FormatterRegistry.class);

        webConfig.addFormatters(registry);

        verify(registry).addConverter(eq(String.class), eq(Instant.class), any());
    }
}
