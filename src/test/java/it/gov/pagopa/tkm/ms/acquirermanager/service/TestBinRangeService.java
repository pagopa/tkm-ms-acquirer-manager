package it.gov.pagopa.tkm.ms.acquirermanager.service;

import com.fasterxml.jackson.databind.*;
import it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.*;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.*;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.*;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.*;
import it.gov.pagopa.tkm.ms.acquirermanager.thread.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.cloud.sleuth.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.test.util.*;

import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class TestBinRangeService {

    @InjectMocks
    private BinRangeServiceImpl binRangeService;

    @Mock
    private BinRangeRepository binRangeRepository;

    @Mock
    private BatchResultRepository batchResultRepository;

    @Mock
    private VisaClient visaClient;

    @Spy
    private ObjectMapper mapper;

    @Mock
    private Tracer tracer;

    @Mock
    private GenBinRangeCallable genBinRangeCallable;

    @Mock
    private BlobServiceImpl blobService;

    private final ArgumentCaptor<TkmBatchResult> batchResultArgumentCaptor = ArgumentCaptor.forClass(TkmBatchResult.class);

    private final DefaultBeans testBeans = new DefaultBeans();

    private final MockedStatic<Instant> instantMockedStatic = mockStatic(Instant.class);

    @BeforeEach
    void init() {
        instantMockedStatic.when(Instant::now).thenReturn(DefaultBeans.INSTANT);
        ReflectionTestUtils.setField(binRangeService, "maxRowsInFiles", 2);
    }

    @AfterAll
    void tearDown() {
        instantMockedStatic.close();
    }

    @Test
    void givenBinRanges_persistResult() {
        when(binRangeRepository.count()).thenReturn(3L);
        when(genBinRangeCallable.call(any(Instant.class), anyInt(), anyInt(), anyLong())).thenReturn(new AsyncResult<>(BatchResultDetails.builder().success(true).build()));
        binRangeService.generateBinRangeFiles();
        verify(batchResultRepository).save(batchResultArgumentCaptor.capture());
        assertThat(batchResultArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("details", "executionTraceId")
                .isEqualTo(testBeans.BIN_RANGE_BATCH_RESULT);
    }

    @Test
    void givenNoBinRanges_persistResult() {
        when(genBinRangeCallable.call(any(Instant.class), anyInt(), anyInt(), anyLong())).thenReturn(new AsyncResult<>(BatchResultDetails.builder().success(true).build()));
        when(binRangeRepository.count()).thenReturn(0L);
        binRangeService.generateBinRangeFiles();
        verify(batchResultRepository).save(batchResultArgumentCaptor.capture());
        assertThat(batchResultArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("details", "executionTraceId")
                .isEqualTo(testBeans.BIN_RANGE_BATCH_RESULT);
    }

    @Test
    void callVisaApiAndWriteResult() throws Exception {
        when(visaClient.getBinRanges()).thenReturn(testBeans.TKM_BIN_RANGES);
        binRangeService.retrieveVisaBinRanges();
        verify(batchResultRepository).save(batchResultArgumentCaptor.capture());
        assertThat(batchResultArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("executionTraceId")
                .isEqualTo(testBeans.VISA_BIN_RANGE_RETRIEVAL_BATCH_RESULT);
    }

    @Test
    void givenVisaApiError_writeFalseOutcome() throws Exception {
        when(visaClient.getBinRanges()).thenThrow(new RuntimeException());
        binRangeService.retrieveVisaBinRanges();
        verify(batchResultRepository).save(batchResultArgumentCaptor.capture());
        assertThat(batchResultArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("executionTraceId")
                .isEqualTo(testBeans.VISA_BIN_RANGE_RETRIEVAL_BATCH_RESULT_FAILED);
    }

}
