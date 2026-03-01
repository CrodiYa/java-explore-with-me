package ru.practicum.ewm.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NullNotBlankValidatorTest {

    private final NullNotBlankValidator validator = new NullNotBlankValidator();

    @Test
    public void shouldReturnTrueForFullString() {
        String login = "login";
        assertTrue(validator.isValid(login, null));
    }

    @Test
    public void shouldReturnTrueForNull() {
        assertTrue(validator.isValid(null, null));
    }

    @Test
    public void shouldReturnFalseForEmpty() {
        assertFalse(validator.isValid("", null));
    }

    @Test
    public void shouldReturnFalseForBlank() {
        assertFalse(validator.isValid("   ", null));
    }
}
