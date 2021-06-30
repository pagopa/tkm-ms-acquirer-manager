package it.gov.pagopa.tkm.ms.acquirermanager.service;

import com.google.common.io.ByteStreams;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.DefaultBeans;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBatchResult;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.BatchResultRepository;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.BatchAcquirerServiceImpl;
import it.gov.pagopa.tkm.ms.acquirermanager.thread.SendBatchAcquirerRecordToQueue;
import it.gov.pagopa.tkm.ms.acquirermanager.util.ObjectMapperUtils;
import it.gov.pagopa.tkm.ms.acquirermanager.util.SftpUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.openpgp.PGPException;
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
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
    private SftpUtils sftpUtils;

    @Mock
    private Tracer tracer;

    @Mock
    private SendBatchAcquirerRecordToQueue sendBatchAcquirerRecordToQueue;

    @Mock
    private Span span;

    @Mock
    private TraceContext traceContext;

    @Mock
    Future<Void> mockFuture;

    @Mock
    private ObjectMapperUtils mapperUtils;

    private final ArgumentCaptor<TkmBatchResult> batchResultArgumentCaptor = ArgumentCaptor.forClass(TkmBatchResult.class);

    private final DefaultBeans testBeans = new DefaultBeans();

    @BeforeEach
    void init() throws IOException {
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(span.context().traceId()).thenReturn(TRACE_ID);
        String privateKey = IOUtils.toString(new ClassPathResource("junit_pgp_private.asc").getInputStream(), StandardCharsets.UTF_8.name());
        ReflectionTestUtils.setField(batchAcquirerService, "pgpPrivateKey", privateKey.getBytes());
        ReflectionTestUtils.setField(batchAcquirerService, "pgpPassPhrase", "passphrase".toCharArray());
    }

    @Test
    void givenRemoteResourceInfo_success() throws IOException, PGPException {
        final UUID defaultUuid = UUID.fromString("8d8b30e3-de52-4f1c-a71c-9905a8043dac");
        try (MockedStatic<UUID> mockedUuid = Mockito.mockStatic(UUID.class)) {
            mockedUuid.when(() -> UUID.randomUUID()).thenReturn(defaultUuid);
            String directory = FileUtils.getTempDirectoryPath() + File.separator + UUID.randomUUID();
            String tempFileZip = directory + File.separator + testBeans.acquirerFileName;
            FileUtils.deleteDirectory(new File(directory));
            byte[] bytes = ByteStreams.toByteArray(new ClassPathResource(testBeans.acquirerFileName).getInputStream());
            when(sftpUtils.listFile()).thenReturn(testBeans.REMOTE_RESOURCE_INFO);
            doAnswer((i) -> {
                FileUtils.writeByteArrayToFile(new File(tempFileZip), bytes);
                return null;
            }).when(sftpUtils).downloadFile(anyString(), anyString());
            when(sendBatchAcquirerRecordToQueue.sendToQueue(anyList())).thenReturn(mockFuture);
            String details = "{}";
            when(mapperUtils.toJsonOrNull(any())).thenReturn(details);
            batchAcquirerService.queueBatchAcquirerResult();
            verify(sftpUtils, times(1)).downloadFile(anyString(), anyString());
            TkmBatchResult build = TkmBatchResult.builder().
                    runOutcome(true)
                    .executionTraceId(TRACE_ID)
                    .details(details)
                    .targetBatch(BatchEnum.BATCH_ACQUIRER)
                    .build();
            verify(batchResultRepository).save(batchResultArgumentCaptor.capture());
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
