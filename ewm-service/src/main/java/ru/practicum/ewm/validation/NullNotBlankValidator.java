package ru.practicum.ewm.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Custom validator for {@link NullNotBlank}
 */
public class NullNotBlankValidator implements ConstraintValidator<NullNotBlank, String> {
    /**
     * Validates that the {@code String} is not blank.
     * <p> {@code null} elements are considered valid.
     *
     * @param s                          string to validate
     * @param constraintValidatorContext validation context
     * @return true if string is not blank, false otherwise
     */
    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        if (s == null) {
            return true;
        }

        return !s.isBlank();
    }
}
