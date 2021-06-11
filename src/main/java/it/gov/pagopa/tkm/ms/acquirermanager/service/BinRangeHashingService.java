package it.gov.pagopa.tkm.ms.acquirermanager.service;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.LinksResponse;

import java.io.*;

public interface BinRangeHashingService {

    LinksResponse getSasLinkResponse(BatchEnum batchEnum);

    void generateBinRangeFiles() throws IOException;

}
