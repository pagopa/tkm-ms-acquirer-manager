package it.gov.pagopa.tkm.ms.acquirermanager.service;

import it.gov.pagopa.tkm.constant.TkmDatetimeConstant;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.DefaultBeans;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBatchResult;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.*;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.KnownHashesCopyServiceImpl;
import it.gov.pagopa.tkm.ms.acquirermanager.util.ObjectMapperUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.cloud.sleuth.Tracer;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class TestKnownHashesCopyService {

    private static final String TRACE_ID = "traceId";

    @InjectMocks
    private KnownHashesCopyServiceImpl knownHashesCopyService;

    @Mock
    private BlobService blobService;

    @Mock
    private BatchResultRepository batchResultRepository;

    @Mock
    private HashOffsetRepository hashOffsetRepository;

    @Mock
    private Tracer tracer;

    @Mock
    private ObjectMapperUtils mapperUtils;

    @Mock
    private Span span;

    @Mock
    private TraceContext traceContext;

    private final ArgumentCaptor<TkmBatchResult> batchResultArgumentCaptor = ArgumentCaptor.forClass(TkmBatchResult.class);

    private final DefaultBeans defaultBeans = new DefaultBeans();
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("uuuuMMdd").withZone(ZoneId.of(TkmDatetimeConstant.DATE_TIME_TIMEZONE));

    @BeforeEach
    void init() throws IOException {
        knownHashesCopyService.init();
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(span.context().traceId()).thenReturn(TRACE_ID);
    }

    @Test
    void copyKnownHashesFiles_success() throws IOException {
        when(blobService.getFilesFromDirectory(anyString())).thenReturn(defaultBeans.BLOB_LIST);
        when(blobService.uploadFile(any(), any(Instant.class), anyString(), anyString(), any(BatchEnum.class))).thenReturn(DefaultBeans.TESTNAME);
        String mapperUtilsString = "{}";
        when(mapperUtils.toJsonOrNull(any())).thenReturn(mapperUtilsString);
        String fileDownloaded = FileUtils.getTempDirectoryPath() + File.separator + DefaultBeans.TESTNAME;
        FileUtils.write(new File(fileDownloaded), "dataFile");
        knownHashesCopyService.copyKnownHashesFiles();
        verify(blobService).uploadFile(any(), any(Instant.class),
                eq(DefaultBeans.TESTNAME.replace("date", dateFormat.format(Instant.now()))),
                Mockito.matches("[a-z0-9]{16}"), eq(BatchEnum.KNOWN_HASHES_COPY));
        TkmBatchResult build = TkmBatchResult.builder().
                runOutcome(true)
                .executionTraceId(TRACE_ID)
                .targetBatch(BatchEnum.KNOWN_HASHES_COPY)
                .details(mapperUtilsString)
                .build();
        verify(batchResultRepository).save(batchResultArgumentCaptor.capture());
        verify(hashOffsetRepository).deleteAll();
        TkmBatchResult value = batchResultArgumentCaptor.getValue();
        assertThat(value)
                .usingRecursiveComparison()
                .ignoringFields("runDate", "runDurationMillis")
                .isEqualTo(build);
        assertTrue(value.getRunDurationMillis() > 0);
        assertNotNull(value.getRunDate());
    }

    @Test
    void copyKnownHashesFiles_exceptionDownload() {
        when(blobService.getFilesFromDirectory(anyString())).thenReturn(defaultBeans.BLOB_LIST);
        String mapperUtilsString = "{\"errorMessage\": \"message\"}";
        when(mapperUtils.toJsonOrNull(any())).thenReturn(mapperUtilsString);
        knownHashesCopyService.copyKnownHashesFiles();
        verify(blobService, never()).uploadFile(any(), any(Instant.class), anyString(), anyString(), any((BatchEnum.class)));
        TkmBatchResult build = TkmBatchResult.builder().
                runOutcome(false)
                .executionTraceId(TRACE_ID)
                .targetBatch(BatchEnum.KNOWN_HASHES_COPY)
                .details(mapperUtilsString)
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