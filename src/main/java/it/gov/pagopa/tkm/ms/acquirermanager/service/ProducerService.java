package it.gov.pagopa.tkm.ms.acquirermanager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.ReadQueue;
import org.bouncycastle.openpgp.PGPException;

public interface ProducerService {

    void sendMessage(ReadQueue readQueue) throws JsonProcessingException, PGPException;

}
