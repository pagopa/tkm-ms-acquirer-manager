package it.gov.pagopa.tkm.ms.acquirermanager.thread;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.DefaultBeans;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.BatchResultDetails;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.FileGeneratorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class TestGenBinRangeCallable {

    @InjectMocks
    private GenBinRangeCallable genBinRangeCallable;

    @Mock
    private FileGeneratorServiceImpl fileGeneratorService;

    private DefaultBeans testBeans;

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(genBinRangeCallable, "profile", "test");
        testBeans = new DefaultBeans();
    }

    @Test
    void whenCalled_returnDetails() throws IOException, ExecutionException, InterruptedException {
        when(fileGeneratorService.generateBinRangesFile(any(Instant.class), anyInt(), anyInt(), anyLong())).thenReturn(testBeans.BIN_RANGE_BATCH_RESULT_DETAILS);
        Future<BatchResultDetails> details = genBinRangeCallable.call(Instant.now(), 3, 0, 0L);
        assertEquals(testBeans.BIN_RANGE_BATCH_RESULT_DETAILS, details.get());
    }

    @Test
    void whenCalled_errorGenerateFile() throws IOException, ExecutionException, InterruptedException {
        String errorMessage = "error";
        when(fileGeneratorService.generateBinRangesFile(any(Instant.class), anyInt(), anyInt(), anyLong())).thenThrow(new IOException(errorMessage));
        Future<BatchResultDetails> details = genBinRangeCallable.call(Instant.now(), 0, 0, 0L);
        assertEquals(testBeans.BIN_RANGE_BATCH_RESULT_ERROR_DETAILS, details.get());
    }

}
