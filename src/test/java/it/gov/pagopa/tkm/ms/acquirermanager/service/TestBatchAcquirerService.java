package it.gov.pagopa.tkm.ms.acquirermanager.service;

import com.azure.core.http.rest.*;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.*;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBatchResult;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.*;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.BatchResultRepository;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.*;
import it.gov.pagopa.tkm.ms.acquirermanager.thread.SendBatchAcquirerRecordToQueue;
import it.gov.pagopa.tkm.ms.acquirermanager.util.ObjectMapperUtils;
import it.gov.pagopa.tkm.service.*;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.openpgp.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.concurrent.*;
import java.util.stream.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class TestBatchAcquirerService {
    private static final String TRACE_ID = "traceId";
    @InjectMocks
    private BatchAcquirerServiceImpl batchAcquirerService;

    @Mock
    private BatchResultRepository batchResultRepository;

    @Mock
    private Tracer tracer;

    @Mock
    private SendBatchAcquirerRecordToQueue sendBatchAcquirerRecordToQueue;

    @Mock
    private Span span;

    @Mock
    private TraceContext traceContext;

    @Mock
    private ObjectMapperUtils mapperUtils;

    @Mock
    private BlobServiceImpl blobService;

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClientMock;

    @Mock
    private PagedIterable<BlobItem> pagedIterableMock;

    @Mock
    Future<Void> mockFuture;

    @BeforeEach
    void init() {
        when(blobService.getBlobContainerClient(any())).thenReturn(blobContainerClient);
        when(blobContainerClient.getBlobClient(any())).thenReturn(blobClientMock);
    }

    private final ArgumentCaptor<TkmBatchResult> batchResultArgumentCaptor = ArgumentCaptor.forClass(TkmBatchResult.class);

    private final DefaultBeans testBeans = new DefaultBeans();

    @Test
    void givenAcquirerFile_success() throws IOException, PGPException {
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(span.context().traceId()).thenReturn(TRACE_ID);
        String privateKey = IOUtils.toString(new ClassPathResource("junit_pgp_private.asc").getInputStream(), StandardCharsets.UTF_8.name());
        ReflectionTestUtils.setField(batchAcquirerService, "pgpPrivateKey", privateKey);
        ReflectionTestUtils.setField(batchAcquirerService, "pgpPassPhrase", "passphrase");
        try (MockedStatic<PgpStaticUtils> pgpStaticUtilsMockedStatic = mockStatic(PgpStaticUtils.class)) {
            when(blobContainerClient.listBlobs()).thenReturn(pagedIterableMock);
            when(pagedIterableMock.stream().collect(Collectors.toList())).thenAnswer(invocation -> Stream.of(new BlobItem()));
            when(mapperUtils.toJsonOrNull(any())).thenReturn("{}");
            pgpStaticUtilsMockedStatic.when(() -> PgpStaticUtils.decryptFileToString(any(), any(), any())).thenReturn(testBeans.ACQUIRER_FILE);
            when(sendBatchAcquirerRecordToQueue.sendToQueue(anyList())).thenReturn(mockFuture);
            batchAcquirerService.queueBatchAcquirerResult();
            TkmBatchResult build = TkmBatchResult.builder().
                    runOutcome(true)
                    .executionTraceId(TRACE_ID)
                    .details("{}")
                    .targetBatch(BatchEnum.BATCH_ACQUIRER)
                    .build();
            verify(batchResultRepository).save(batchResultArgumentCaptor.capture());
            verify(sendBatchAcquirerRecordToQueue).sendToQueue(anyList());
            TkmBatchResult value = batchResultArgumentCaptor.getValue();
            assertThat(value)
                    .usingRecursiveComparison()
                    .ignoringFields("runDate", "runDurationMillis")
                    .isEqualTo(build);
            assertTrue(value.getRunDurationMillis() > 0);
            assertNotNull(value.getRunDate());
        }
    }

    @Test
    void getPublicPgpKey_success() {
        String key = batchAcquirerService.getPublicPgpKey();
        verify(blobClientMock).download(any(ByteArrayOutputStream.class));
        assertEquals("", key);
    }

    @Test
    void uploadFile_success() throws IOException {
        ReflectionTestUtils.setField(batchAcquirerService, "chunkSize", 10L);
        ReflectionTestUtils.setField(batchAcquirerService, "maxConcurrency", 2);
        ReflectionTestUtils.setField(batchAcquirerService, "timeLimit", 5L);
        TokenListUploadResponse resp = batchAcquirerService.uploadFile(new MockMultipartFile("FILENAME", new byte[]{100}));
        verify(blobClientMock).uploadWithResponse(any(ByteArrayInputStream.class), anyLong(), any(ParallelTransferOptions.class), any(BlobHttpHeaders.class), any(), any(AccessTier.class), any(BlobRequestConditions.class), any(Duration.class), any());
        assertNotNull(resp);
    }

}
