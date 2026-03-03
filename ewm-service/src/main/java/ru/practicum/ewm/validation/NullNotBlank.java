package ru.practicum.ewm.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that  {@code String} is not blank.
 * <p> {@code null} elements are considered valid.
 *
 * <p>Applied to {@code String} fields to ensure the data not blank.
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NullNotBlankValidator.class)
public @interface NullNotBlank {
    String message() default "String can`t be blank";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
