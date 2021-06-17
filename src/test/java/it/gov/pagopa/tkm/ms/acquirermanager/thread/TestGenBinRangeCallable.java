package it.gov.pagopa.tkm.ms.acquirermanager.thread;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.*;
import it.gov.pagopa.tkm.ms.acquirermanager.service.*;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;

import java.io.*;
import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
public class TestGenBinRangeCallable {

    @InjectMocks
    private GenBinRangeCallable genBinRangeCallable;

    @Mock
    private FileGeneratorServiceImpl fileGeneratorService;

    private DefaultBeans testBeans;

    private final MockedStatic<Instant> instantMockedStatic = mockStatic(Instant.class);

    @BeforeEach
    void init() {
        testBeans = new DefaultBeans();
        instantMockedStatic.when(Instant::now).thenReturn(DefaultBeans.INSTANT);
    }

    @AfterAll
    void tearDown() {
        instantMockedStatic.close();
    }

    @Test
    void whenCalled_returnDetails() throws IOException {
        lenient().when(fileGeneratorService.generateFileWithStream(any(Instant.class), anyInt(), anyInt(), anyLong(), any(BlobService.class))).thenReturn(testBeans.BIN_RANGE_BATCH_RESULT_DETAILS);
        BatchResultDetails details = genBinRangeCallable.call();
        assertThat(details)
                .usingRecursiveComparison()
                .ignoringFields("sha256")
                .isEqualTo(testBeans.BIN_RANGE_BATCH_RESULT_DETAILS);
    }

}
