package it.gov.pagopa.tkm.ms.acquirermanager.service;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.*;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.*;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.data.domain.*;
import org.springframework.test.util.*;

import javax.persistence.*;
import java.io.*;
import java.time.format.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
public class TestFileGeneratorService {

    @InjectMocks
    private FileGeneratorServiceImpl fileGeneratorService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private DateTimeFormatter dateFormatMock;

    @Mock
    private BinRangeRepository binRangeRepository;

    @Mock
    private BlobServiceImpl blobService;

    private DefaultBeans testBeans;

    @BeforeEach
    void init() {
        testBeans = new DefaultBeans();
        ReflectionTestUtils.setField(fileGeneratorService, "dateFormat", dateFormatMock);
        ReflectionTestUtils.setField(fileGeneratorService, "profile", "local");
    }

    @Test
    void givenBinRangeStream_generateFile() throws IOException {
        when(binRangeRepository.getAll(any(PageRequest.class))).thenReturn(testBeans.TKM_BIN_RANGES.stream());
        BatchResultDetails details = fileGeneratorService.generateFileWithStream(DefaultBeans.INSTANT, 3, 0, 5, blobService);
        assertThat(testBeans.BIN_RANGE_BATCH_RESULT_DETAILS)
                .usingRecursiveComparison()
                .ignoringFields("sha256")
                .isEqualTo(details);
    }

    @Test
    void givenNoBinRanges_generateFile() throws IOException {
        BatchResultDetails details = fileGeneratorService.generateFileWithStream(DefaultBeans.INSTANT, 0, 0, 0, blobService);
        assertThat(testBeans.BIN_RANGE_BATCH_RESULT_DETAILS_EMPTY)
                .usingRecursiveComparison()
                .ignoringFields("sha256")
                .isEqualTo(details);
    }

}
