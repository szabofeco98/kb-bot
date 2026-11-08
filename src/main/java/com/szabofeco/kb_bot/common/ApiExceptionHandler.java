package com.szabofeco.kb_bot.common;

import com.szabofeco.kb_bot.ask.AnswerGenerationException;
import com.szabofeco.kb_bot.ingestion.InvalidDocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Arrays;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidDocumentException.class)
    public ProblemDetail handleInvalidDocument(InvalidDocumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Bean Validation on controller method parameters (e.g. {@code @NotBlank question}, {@code @MarkdownFile file}).
     * Detail format: {@code "question: must not be blank; file: must be a non-empty Markdown (.md) file..."}
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleValidation(HandlerMethodValidationException ex) {
        String detail = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> result.getMethodParameter().getParameterName() + ": "
                                + error.getDefaultMessage()))
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    /** Bean Validation on a {@code @Valid @RequestBody}, e.g. POST /api/ask with a blank question. Same format. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleBodyValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    /** Malformed JSON or an unknown enum value (e.g. "product": "AUTOHITEL") in the request body. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Malformed request body. Check the JSON syntax; product must be one of "
                        + Arrays.toString(Product.values()) + " or null.");
    }

    /** The LLM call failed or returned unusable output. Fail closed: no partial answer, details only in the log. */
    @ExceptionHandler(AnswerGenerationException.class)
    public ProblemDetail handleAnswerGeneration(AnswerGenerationException ex) {
        log.error("Answer generation failed", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "The answer could not be generated. Please try again later.");
    }

    /** Missing request parameter, e.g. GET /api/search without "question". */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParameter(MissingServletRequestParameterException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Missing required parameter: " + ex.getParameterName());
    }

    /** E.g. unknown product: product=AUTOHITEL */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String detail = "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue();
        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            detail += ". Allowed values: " + Arrays.toString(ex.getRequiredType().getEnumConstants());
        }
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    /** Missing "file" or "product" part in the multipart request. */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail handleMissingPart(MissingServletRequestPartException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Missing required part: " + ex.getRequestPartName());
    }
}
