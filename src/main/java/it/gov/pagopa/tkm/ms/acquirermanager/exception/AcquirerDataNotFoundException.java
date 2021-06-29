package it.gov.pagopa.tkm.ms.acquirermanager.exception;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;

public class AcquirerDataNotFoundException extends AcquirerException {

    public AcquirerDataNotFoundException(ErrorCodeEnum errorCode) {
        super(errorCode);
    }

}

