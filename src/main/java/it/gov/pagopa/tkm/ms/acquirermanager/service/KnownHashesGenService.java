package it.gov.pagopa.tkm.ms.acquirermanager.service;

import org.springframework.transaction.annotation.*;

public interface KnownHashesGenService {

    @Transactional
    void generateKnownHashesFiles();

}
