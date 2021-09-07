package it.gov.pagopa.tkm.ms.acquirermanager.thread;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.BatchAcquirerCSVRecord;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.CircuitEnum;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.ReadQueue;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.Token;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.ProducerServiceImpl;
import org.bouncycastle.openpgp.PGPException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class TestSendBatchAcquirerRecordToQueue {
    @InjectMocks
    private SendBatchAcquirerRecordToQueue sendBatchAcquirerRecordToQueue;

    @Mock
    private ProducerServiceImpl producerService;

    @Test
    void sendToQueue_success() throws PGPException, JsonProcessingException {
        CircuitEnum visa = CircuitEnum.VISA;
        String par = "par";
        String token = "token";
        BatchAcquirerCSVRecord build = BatchAcquirerCSVRecord.builder()
                .circuit(visa)
                .par(par)
                .token(token)
                .build();
        Token tokenObj = Token.builder().token(token).build();
        ReadQueue readQueue = ReadQueue.builder().circuit(visa).par(par).tokens(Collections.singletonList(tokenObj)).build();
        sendBatchAcquirerRecordToQueue.sendToQueue(Collections.singletonList(build));
        Mockito.verify(producerService).sendMessage(readQueue);
    }

    @Test
    void sendToQueue_skip() throws PGPException, JsonProcessingException {
        CircuitEnum visa = CircuitEnum.UNKNOWN;
        String par = "par";
        String token = "token";
        BatchAcquirerCSVRecord build = BatchAcquirerCSVRecord.builder()
                .circuit(visa)
                .par(par)
                .token(token)
                .build();
        sendBatchAcquirerRecordToQueue.sendToQueue(Collections.singletonList(build));
        Mockito.verify(producerService, Mockito.never()).sendMessage(Mockito.any());
    }
}