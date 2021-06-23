package it.gov.pagopa.tkm.ms.acquirermanager.service;

import org.springframework.transaction.annotation.*;

public interface BinRangeService {

    void generateBinRangeFiles();

    @Transactional
    void retrieveVisaBinRanges();

}
