package it.gov.pagopa.tkm.ms.acquirermanager.service;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.LinksResponse;
import org.springframework.transaction.annotation.*;

public interface BinRangeService {

    LinksResponse getSasLinkResponse(BatchEnum batchEnum);

    void generateBinRangeFiles();

    @Transactional
    void retrieveVisaBinRanges();

}
