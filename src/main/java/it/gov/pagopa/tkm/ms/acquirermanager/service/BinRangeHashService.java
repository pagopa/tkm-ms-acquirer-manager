package it.gov.pagopa.tkm.ms.acquirermanager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.LinksResponse;

public interface BinRangeHashService {

    LinksResponse getSasLinkResponse(BatchEnum batchEnum);

    void generateBinRangeFiles() throws JsonProcessingException;
    void generateHpanHtokenFiles() throws JsonProcessingException;

}
