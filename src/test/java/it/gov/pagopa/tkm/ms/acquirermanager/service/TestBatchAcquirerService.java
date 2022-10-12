package it.gov.pagopa.tkm.ms.acquirermanager.service;

import com.azure.core.http.rest.*;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.*;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBatchResult;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.BatchResultRepository;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.*;
import it.gov.pagopa.tkm.ms.acquirermanager.thread.SendBatchAcquirerRecordToQueue;
import it.gov.pagopa.tkm.ms.acquirermanager.util.ObjectMapperUtils;
import it.gov.pagopa.tkm.service.*;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.openpgp.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.stream.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    private final ArgumentCaptor<TkmBatchResult> batchResultArgumentCaptor = ArgumentCaptor.forClass(TkmBatchResult.class);

    private final DefaultBeans testBeans = new DefaultBeans();

    @BeforeEach
    void init() throws IOException {
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(span.context().traceId()).thenReturn(TRACE_ID);
        String privateKey = IOUtils.toString(new ClassPathResource("junit_pgp_private.asc").getInputStream(), StandardCharsets.UTF_8.name());
        ReflectionTestUtils.setField(batchAcquirerService, "pgpPrivateKey", privateKey);
        ReflectionTestUtils.setField(batchAcquirerService, "pgpPassPhrase", "passphrase");
    }

    @Test
    void givenAcquirerFile_success() throws IOException, PGPException {
        try (MockedStatic<PgpStaticUtils> pgpStaticUtilsMockedStatic = mockStatic(PgpStaticUtils.class)) {
            when(blobService.getBlobContainerClient(any())).thenReturn(blobContainerClient);
            when(blobContainerClient.listBlobs()).thenReturn(pagedIterableMock);
            when(pagedIterableMock.stream().collect(Collectors.toList())).thenAnswer(invocation -> Stream.of(new BlobItem()));
            when(blobContainerClient.getBlobClient(any())).thenReturn(blobClientMock);
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

}
