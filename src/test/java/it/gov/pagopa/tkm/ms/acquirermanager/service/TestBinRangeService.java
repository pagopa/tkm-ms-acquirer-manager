package it.gov.pagopa.tkm.ms.acquirermanager.service;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.*;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.exception.*;
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
import java.time.format.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class TestBinRangeService {

    @InjectMocks
    private BinRangeServiceImpl binRangeService;

    @Mock
    private BlobServiceClientBuilder serviceClientBuilderMock;

    @Mock
    private BlobClientBuilder blobClientBuilderMock;

    @Mock
    private BlobClient blobClientMock;

    @Mock
    private BlobServiceClient serviceClientMock;

    @Mock
    private BlobContainerClient containerClientMock;

    @Mock
    private PagedIterable<BlobItem> pagedIterableMock;

    @Mock
    private DateTimeFormatter dateFormatMock;

    @Mock
    private BinRangeRepository binRangeRepository;

    @Mock
    private BatchResultRepository batchResultRepository;

    @Mock
    private FileGeneratorService fileGeneratorService;
    
    @Mock
    private VisaClient visaClient;

    @Spy
    private ObjectMapper mapper;

    @Mock
    private Tracer tracer;

    @Mock
    private GenBinRangeCallable genBinRangeCallable;


    private final ArgumentCaptor<TkmBatchResult> batchResultArgumentCaptor = ArgumentCaptor.forClass(TkmBatchResult.class);

    private static final UUID UUID_TEST = UUID.fromString("c1f77e6e-8fc7-42d2-8128-58ca293e3b42");

    private final DefaultBeans testBeans = new DefaultBeans();

    private final MockedStatic<Instant> instantMockedStatic = mockStatic(Instant.class);
    private final MockedStatic<OffsetDateTime> offsetMockedStatic = mockStatic(OffsetDateTime.class);
    private final MockedStatic<UUID> mockedUuid = mockStatic(UUID.class);

    @BeforeEach
    void init() {
        instantMockedStatic.when(Instant::now).thenReturn(DefaultBeans.INSTANT);
        offsetMockedStatic.when(OffsetDateTime::now).thenReturn(DefaultBeans.OFFSET_DATE_TIME);
        mockedUuid.when(UUID::randomUUID).thenReturn(UUID_TEST);
        ReflectionTestUtils.setField(binRangeService, "connectionString", DefaultBeans.TEST_CONNECTION_STRING);
        ReflectionTestUtils.setField(binRangeService, "containerName", DefaultBeans.TEST_CONTAINER_NAME);
        ReflectionTestUtils.setField(binRangeService, "dateFormat", dateFormatMock);
        ReflectionTestUtils.setField(binRangeService, "serviceClientBuilder", serviceClientBuilderMock);
        ReflectionTestUtils.setField(binRangeService, "blobClientBuilder", blobClientBuilderMock);
        ReflectionTestUtils.setField(binRangeService, "maxRowsInFiles", 2);
    }

    @AfterAll
    void tearDown() {
        instantMockedStatic.close();
        offsetMockedStatic.close();
    }

    //BIN RANGE FILE RETRIEVAL

    private void startupAssumptions(boolean shouldThrowException) {
        when(serviceClientBuilderMock.connectionString(DefaultBeans.TEST_CONNECTION_STRING)).thenReturn(serviceClientBuilderMock);
        when(serviceClientBuilderMock.buildClient()).thenReturn(serviceClientMock);
        if (!shouldThrowException) {
            when(blobClientBuilderMock.connectionString(DefaultBeans.TEST_CONNECTION_STRING)).thenReturn(blobClientBuilderMock);
            when(blobClientBuilderMock.containerName(DefaultBeans.TEST_CONTAINER_NAME)).thenReturn(blobClientBuilderMock);
            when(blobClientBuilderMock.blobName(notNull())).thenReturn(blobClientBuilderMock);
            when(blobClientBuilderMock.buildClient()).thenReturn(blobClientMock);
        }
        when(serviceClientMock.getBlobContainerClient(DefaultBeans.TEST_CONTAINER_NAME)).thenReturn(containerClientMock);
        when(containerClientMock.listBlobsByHierarchy(nullable(String.class), nullable(ListBlobsOptions.class), nullable(Duration.class))).thenReturn(pagedIterableMock);
        when(dateFormatMock.format(DefaultBeans.INSTANT)).thenReturn("20210101");
    }

    @Test
    void givenExistingFile_returnLinksResponseBinRanges() {
        startupAssumptions(false);
        when(pagedIterableMock.iterator()).thenReturn(testBeans.BLOB_LIST.iterator());
        assertEquals(testBeans.LINKS_RESPONSE, binRangeService.getSasLinkResponse(BatchEnum.BIN_RANGE_GEN));
    }

    @Test
    void givenExistingFile_returnLinksResponseHashes() {
        startupAssumptions(false);
        when(pagedIterableMock.iterator()).thenReturn(testBeans.BLOB_LIST.iterator());
        assertEquals(testBeans.LINKS_RESPONSE, binRangeService.getSasLinkResponse(BatchEnum.KNOWN_HASHES_GEN));
    }

    @Test
    void givenNoFiles_expectNotFoundException() {
        startupAssumptions(true);
        when(pagedIterableMock.iterator()).thenReturn(Collections.emptyIterator());
        assertThrows(AcquirerDataNotFoundException.class, () -> binRangeService.getSasLinkResponse(BatchEnum.BIN_RANGE_GEN));
        assertThrows(AcquirerDataNotFoundException.class, () -> binRangeService.getSasLinkResponse(BatchEnum.KNOWN_HASHES_GEN));
    }

    //BIN RANGE FILE GENERATION

    @Test
    void givenBinRanges_persistResult() throws JsonProcessingException {
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
    void givenNoBinRanges_persistResult() throws JsonProcessingException {
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
    void callVisaApiAndWriteResult() throws JsonProcessingException {
        when(visaClient.getBinRanges()).thenReturn(testBeans.TKM_BIN_RANGES);
        binRangeService.retrieveVisaBinRanges();
        verify(batchResultRepository).save(batchResultArgumentCaptor.capture());
        assertThat(batchResultArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("executionTraceId")
                .isEqualTo(testBeans.VISA_BIN_RANGE_RETRIEVAL_BATCH_RESULT);
    }

    @Test
    void givenVisaApiError_writeFalseOutcome() throws JsonProcessingException {
        when(visaClient.getBinRanges()).thenThrow(new RuntimeException());
        binRangeService.retrieveVisaBinRanges();
        verify(batchResultRepository).save(batchResultArgumentCaptor.capture());
        assertThat(batchResultArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("executionTraceId")
                .isEqualTo(testBeans.VISA_BIN_RANGE_RETRIEVAL_BATCH_RESULT_FAILED);
    }

}
