package ru.yandex.practicum.filmorate.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MinimumDateValidator.class)
public @interface MinimumDate {
    String message() default "Дата не может быть раньше {value}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String value() default "1895-12-28";
}