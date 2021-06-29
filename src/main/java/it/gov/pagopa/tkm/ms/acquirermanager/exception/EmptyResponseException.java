package it.gov.pagopa.tkm.ms.acquirermanager.exception;


public class EmptyResponseException extends RuntimeException {

    public EmptyResponseException(String message) {
        super(message);
    }
}
