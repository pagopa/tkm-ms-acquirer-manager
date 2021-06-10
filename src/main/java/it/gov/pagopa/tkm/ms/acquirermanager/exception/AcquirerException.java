package it.gov.pagopa.tkm.ms.acquirermanager.exception;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
public class AcquirerException extends RuntimeException {

    private ErrorCodeEnum errorCode;

    public AcquirerException(ErrorCodeEnum ec) {
        super(ec.getStatusCode() + " - " + ec.getMessage());
        errorCode = ec;
    }

}
