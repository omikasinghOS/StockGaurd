package com.stockguard.shared;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
@RestControllerAdvice
public class ApiErrors {
 private ResponseEntity<ProblemDetail> error(HttpStatus status, String detail) {
  var p = ProblemDetail.forStatusAndDetail(status, detail); p.setProperty("correlationId", MDC.get("correlationId")); return ResponseEntity.status(status).body(p);
 }
 @ExceptionHandler(ApiException.class) ResponseEntity<ProblemDetail> api(ApiException e) { return error(e.status,e.getMessage()); }
 @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
 ResponseEntity<ProblemDetail> invalid(Exception e) { return error(HttpStatus.BAD_REQUEST,"Invalid request. Check required fields, types and limits."); }
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<ProblemDetail> conflict(Exception e) { return error(HttpStatus.CONFLICT,"The request conflicts with an existing record or constraint."); }
}
