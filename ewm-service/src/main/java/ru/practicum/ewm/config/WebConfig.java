package ru.practicum.ewm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ru.practicum.dto.Formatter;

import java.time.Instant;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToInstantConverter());
    }

    private static class StringToInstantConverter implements Converter<String, Instant> {
        @Override
        public Instant convert(String source) {
            return Formatter.toInstant(source);
        }
    }
}