package local.aidev.api;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import io.temporal.client.*;

import java.io.*;
import java.nio.file.NoSuchFileException;
import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<Map<String, Object>> badRequest(Exception e) {
        return error(HttpStatus.BAD_REQUEST, e);
    }

    @ExceptionHandler({NoSuchFileException.class})
    ResponseEntity<Map<String, Object>> notFound(Exception e) {
        return error(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler({WorkflowNotFoundException.class})
    ResponseEntity<Map<String, Object>> workflowNotFound(Exception e) {
        return error(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler({WorkflowUpdateException.class})
    ResponseEntity<Map<String, Object>> invalidUpdate(Exception e) {
        return error(HttpStatus.CONFLICT, e);
    }

    @ExceptionHandler({WorkflowServiceException.class})
    ResponseEntity<Map<String, Object>> temporalUnavailable(Exception e) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, e);
    }

    @ExceptionHandler({IllegalStateException.class})
    ResponseEntity<Map<String, Object>> conflict(Exception e) {
        return error(HttpStatus.CONFLICT, e);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> unexpected(Exception e) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, e);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, Exception e) {
        return ResponseEntity.status(status).body(Map.of("timestamp", Instant.now().toString(), "status", status.value(),
                "error", status.getReasonPhrase(), "message", Objects.toString(e.getMessage(), "Request failed")));
    }
}
