package it.gov.pagopa.tkm.ms.acquirermanager.config;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.exception.*;
import lombok.extern.log4j.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
@Log4j2
public class ErrorHandler {

    @ExceptionHandler(AcquirerDataNotFoundException.class)
    public ResponseEntity<Void> handleAcquirerDataNotFoundException(AcquirerDataNotFoundException ce) {
        log.error(ce.getMessage());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(AcquirerException.class)
    public ResponseEntity<ErrorCodeEnum> handleAcquirerException(AcquirerException ce) {
        log.error(ce.getMessage());
        return ResponseEntity.badRequest().body(ce.getErrorCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleException(Exception e) {
        log.error(e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

}
