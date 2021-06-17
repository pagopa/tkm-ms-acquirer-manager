package it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.DefaultBeans;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestVisaClient {
    @InjectMocks
    private VisaClient visaClient;

    @Spy
    private ObjectMapper mapper;

    private DefaultBeans testBeans;

    @BeforeEach()
    void init() {
        testBeans = new DefaultBeans();
        mapper.registerModule(new JavaTimeModule());
    }
}