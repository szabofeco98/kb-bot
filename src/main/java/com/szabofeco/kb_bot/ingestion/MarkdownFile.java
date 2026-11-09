package com.szabofeco.kb_bot.ingestion;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = MarkdownFileValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MarkdownFile {

    String message() default "must be a non-empty Markdown (.md) file; convert other formats (PDF, Word) "
            + "to Markdown first";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
