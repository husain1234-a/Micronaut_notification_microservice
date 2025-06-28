package com.yash.notification.exception;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.exceptions.DataAccessException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Produces
@Singleton
@Requires(classes = {Exception.class, ExceptionHandler.class})
public class GlobalExceptionHandler implements ExceptionHandler<Exception, HttpResponse<ErrorResponse>> {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, Exception exception) {
        LOG.error("Error occurred while processing request: {}", request.getPath(), exception);

        if (exception instanceof ResourceNotFoundException) {
            return HttpResponse.notFound(new ErrorResponse(exception.getMessage(), request.getPath()));
        } else if (exception instanceof ValidationException) {
            return HttpResponse.badRequest(new ErrorResponse("Validation error: " + exception.getMessage(), request.getPath()));
        } else if (exception instanceof DuplicateResourceException) {
            return HttpResponse.status(HttpStatus.CONFLICT).body(new ErrorResponse(exception.getMessage(), request.getPath()));
        } else if (exception instanceof DatabaseException) {
            return HttpResponse.serverError(new ErrorResponse("Database error: " + exception.getMessage(), request.getPath()));
        } else if (exception instanceof DataAccessException) {
            return HttpResponse.serverError(new ErrorResponse("Database error occurred", request.getPath()));
        } else {
            return HttpResponse.serverError(new ErrorResponse("Internal server error", request.getPath()));
        }
    }
}

//class ErrorResponse {
//    private String message;
//    private String path;
//    private String timestamp;
//
//    public ErrorResponse(String message, String path) {
//        this.message = message;
//        this.path = path;
//        this.timestamp = java.time.LocalDateTime.now().toString();
//    }
//
//    public String getMessage() {
//        return message;
//    }
//
//    public void setMessage(String message) {
//        this.message = message;
//    }
//
//    public String getPath() {
//        return path;
//    }
//
//    public void setPath(String path) {
//        this.path = path;
//    }
//
//    public String getTimestamp() {
//        return timestamp;
//    }
//
//    public void setTimestamp(String timestamp) {
//        this.timestamp = timestamp;
//    }
//}