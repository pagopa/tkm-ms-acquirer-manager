package it.gov.pagopa.tkm.ms.acquirermanager.thread;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.DefaultBeans;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.BatchResultDetails;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BlobService;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.BlobServiceImpl;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class TestGenBinRangeCallable {

    @InjectMocks
    private GenBinRangeCallable genBinRangeCallable = new GenBinRangeCallable(null, Instant.now(), new BlobServiceImpl(), 0, 0, 0);

    @Mock
    private FileGeneratorServiceImpl fileGeneratorService;

    private DefaultBeans testBeans;

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(genBinRangeCallable, "fileGeneratorService", fileGeneratorService);
        testBeans = new DefaultBeans();
    }

    @Test
    void whenCalled_returnDetails() throws IOException {
        when(fileGeneratorService.generateFileWithStream(any(Instant.class), anyInt(), anyInt(), anyLong(), any(BlobService.class))).thenReturn(testBeans.BIN_RANGE_BATCH_RESULT_DETAILS);
        BatchResultDetails details = genBinRangeCallable.call();
        assertEquals(testBeans.BIN_RANGE_BATCH_RESULT_DETAILS, details);
    }

}
