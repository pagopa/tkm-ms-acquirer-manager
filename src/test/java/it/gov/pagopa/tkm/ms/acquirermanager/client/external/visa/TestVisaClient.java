package it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.DefaultBeans;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.*;
import okhttp3.mockwebserver.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.*;
import org.springframework.test.util.*;
import org.springframework.web.client.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class TestVisaClient {

    @InjectMocks
    private VisaClient visaClient;

    @Spy
    private ObjectMapper mapper;

    @Spy
    private RestTemplate restTemplate;

    private DefaultBeans testBeans;

    private static MockWebServer mockServer;

    @BeforeAll
    static void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
    }

    @BeforeEach()
    void init() throws Exception {
        testBeans = new DefaultBeans();
        mapper.registerModule(new JavaTimeModule());
        ReflectionTestUtils.setField(visaClient, "publicCert", new ClassPathResource("public_cert_test.p12"));
        ReflectionTestUtils.setField(visaClient, "keystorePassword", "password");
        ReflectionTestUtils.setField(visaClient, "userId", "TEST_USER_ID");
        ReflectionTestUtils.setField(visaClient, "password", "TEST_PASSWORD");
        ReflectionTestUtils.setField(visaClient, "keyId", "TEST_KEY_ID");
        ReflectionTestUtils.setField(visaClient, "retrieveBinRangesUrl", "http://localhost:" + mockServer.getPort());
        ReflectionTestUtils.setField(visaClient, "mapper", mapper);
        Method postConstruct = VisaClient.class.getDeclaredMethod("init");
        postConstruct.setAccessible(true);
        postConstruct.invoke(visaClient);
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockServer.shutdown();
    }

    @Test
    void givenRequest_callClient() throws JsonProcessingException {
        MockResponse mockResponse1 = new MockResponse()
                .setBody(mapper.writeValueAsString(testBeans.VISA_BIN_RANGE_RESPONSE))
                .addHeader("Content-Type", "application/json");
        MockResponse mockResponse2 = new MockResponse()
                .setBody(mapper.writeValueAsString(testBeans.VISA_BIN_RANGE_RESPONSE_LAST))
                .addHeader("Content-Type", "application/json");
        mockServer.enqueue(mockResponse1);
        mockServer.enqueue(mockResponse2);
        List<TkmBinRange> binRanges = visaClient.getBinRanges();
        assertThat(testBeans.VISA_TKM_BIN_RANGES)
                .usingRecursiveComparison()
                .ignoringFields("insertDate")
                .isEqualTo(binRanges);
        binRanges.forEach(b -> assertNotNull(b.getInsertDate()));
    }

}