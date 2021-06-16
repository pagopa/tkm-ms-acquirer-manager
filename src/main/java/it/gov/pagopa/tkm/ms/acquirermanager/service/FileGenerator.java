package it.gov.pagopa.tkm.ms.acquirermanager.service;


import java.time.Instant;

public interface FileGenerator {
    void generateFileWithStream(Instant now, int page, int size, int index);
}
