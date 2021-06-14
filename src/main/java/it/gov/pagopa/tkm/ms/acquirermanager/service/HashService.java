package it.gov.pagopa.tkm.ms.acquirermanager.service;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.LinksResponse;

import java.io.IOException;

public interface HashService {

    LinksResponse getSasLinkResponse(BatchEnum batchEnum);

    void generateFiles(BatchEnum batchEnum) throws IOException;

}
