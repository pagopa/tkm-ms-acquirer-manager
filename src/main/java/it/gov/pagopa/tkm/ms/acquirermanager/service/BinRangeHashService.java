package it.gov.pagopa.tkm.ms.acquirermanager.service;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.LinksResponse;
import org.springframework.transaction.annotation.*;

import java.io.*;

public interface BinRangeHashService {

    LinksResponse getSasLinkResponse(BatchEnum batchEnum);

    void generateBinRangeFiles() throws IOException;

    @Transactional
    void retrieveVisaBinRanges() throws Exception;

}
