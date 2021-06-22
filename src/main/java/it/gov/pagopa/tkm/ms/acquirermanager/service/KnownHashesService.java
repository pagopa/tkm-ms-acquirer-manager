package it.gov.pagopa.tkm.ms.acquirermanager.service;

import com.fasterxml.jackson.core.*;
import org.springframework.transaction.annotation.*;

public interface KnownHashesService {

    @Transactional
    void generateKnownHashesFiles() throws JsonProcessingException;

}
