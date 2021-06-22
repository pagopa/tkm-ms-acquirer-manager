package it.gov.pagopa.tkm.ms.acquirermanager.service;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.DefaultBeans;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.BatchResultDetails;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.BinRangeRepository;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.BlobServiceImpl;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.FileGeneratorServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import javax.persistence.EntityManager;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class TestFileGeneratorService {

    @InjectMocks
    private FileGeneratorServiceImpl fileGeneratorService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private BinRangeRepository binRangeRepository;

    @Mock
    private BlobServiceImpl blobService;

    private DefaultBeans testBeans;

    @BeforeEach
    void init() {
        testBeans = new DefaultBeans();
    }

    @Test
    void givenBinRangeStream_generateFile() throws IOException {
        when(binRangeRepository.getAll(any(PageRequest.class))).thenReturn(testBeans.TKM_BIN_RANGES.stream());
        BatchResultDetails details = fileGeneratorService.generateBinRangesFile(DefaultBeans.INSTANT, 5, 0, 5);
        assertThat(testBeans.BIN_RANGE_BATCH_RESULT_DETAILS)
                .usingRecursiveComparison()
                .ignoringFields("sha256")
                .isEqualTo(details);
        Assertions.assertNotNull(details.getSha256());
        Assertions.assertTrue(details.isSuccess());
    }

    @Test
    void givenNoBinRanges_generateFile() throws IOException {
        BatchResultDetails details = fileGeneratorService.generateBinRangesFile(DefaultBeans.INSTANT, 0, 0, 0);
        assertThat(testBeans.BIN_RANGE_BATCH_RESULT_DETAILS_EMPTY)
                .usingRecursiveComparison()
                .ignoringFields("sha256")
                .isEqualTo(details);
        Assertions.assertNotNull(details.getSha256());
        Assertions.assertTrue(details.isSuccess());


    }

}
