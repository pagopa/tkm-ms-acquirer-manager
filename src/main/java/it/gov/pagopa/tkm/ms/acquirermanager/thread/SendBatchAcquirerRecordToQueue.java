package it.gov.pagopa.tkm.ms.acquirermanager.thread;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.BatchAcquirerCSVRecord;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.ReadQueue;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.Token;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.ProducerServiceImpl;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.openpgp.PGPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

@Log4j2
@Component
public class SendBatchAcquirerRecordToQueue {

    @Autowired
    private ProducerServiceImpl producerService;

    @Async
    public Future<Void> sendToQueue(List<BatchAcquirerCSVRecord> parsedRows) throws PGPException, JsonProcessingException {
        for (BatchAcquirerCSVRecord row : parsedRows) {
            if (!row.getCircuit().isAllowedValue())
                continue;

            ReadQueue message = new ReadQueue();
            message.setCircuit(row.getCircuit());
            message.setPar(row.getPar());
            List<Token> tokens = new ArrayList<>();
            tokens.add(new Token(row.getToken(), null));
            message.setTokens(tokens);
            producerService.sendMessage(message);
        }
        return null;
    }
}