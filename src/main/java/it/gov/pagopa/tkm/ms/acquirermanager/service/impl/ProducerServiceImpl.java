package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.ReadQueue;
import it.gov.pagopa.tkm.ms.acquirermanager.service.ProducerService;
import it.gov.pagopa.tkm.service.*;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.openpgp.PGPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class ProducerServiceImpl implements ProducerService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper mapper;

    @Value("${spring.kafka.topics.read-queue.name}")
    private String readQueueTopic;

    @Value("${keyvault.readQueuePubPgpKey}")
    private byte[] readQueuePubPgpKey;

    public void sendMessage(ReadQueue readQueue) throws JsonProcessingException, PGPException {
        String message = mapper.writeValueAsString(readQueue);
        log.trace("Forwarding message to queue: " + message);
        byte[] encryptedMessage = PgpStaticUtils.encrypt(message.getBytes(), readQueuePubPgpKey, true);

        kafkaTemplate.send(readQueueTopic, new String(encryptedMessage));
    }

}
