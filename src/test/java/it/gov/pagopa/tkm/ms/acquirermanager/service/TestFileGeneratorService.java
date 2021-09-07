package it.gov.pagopa.tkm.ms.acquirermanager.service;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum;
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
import org.springframework.test.util.ReflectionTestUtils;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
        ReflectionTestUtils.setField(fileGeneratorService, "profile", "local");
    }

    @Test
    void givenBinRangeStream_generateFile() throws IOException {
        when(binRangeRepository.getAll(any(PageRequest.class))).thenReturn(testBeans.TKM_BIN_RANGES.stream());
        BatchResultDetails details = fileGeneratorService.generateBinRangesFile(DefaultBeans.INSTANT, 5, 0, 5);
        assertThat(testBeans.BIN_RANGE_BATCH_RESULT_DETAILS_NO_DATE)
                .usingRecursiveComparison()
                .ignoringFields("sha256")
                .isEqualTo(details);
        Assertions.assertNotNull(details.getSha256());
        Assertions.assertTrue(details.isSuccess());
    }

    @Test
    void givenNoBinRanges_generateFile() throws IOException {
        BatchResultDetails details = fileGeneratorService.generateBinRangesFile(DefaultBeans.INSTANT, 0, 0, 0);
        assertThat(testBeans.BIN_RANGE_BATCH_RESULT_DETAILS_EMPTY_NO_DATE)
                .usingRecursiveComparison()
                .ignoringFields("sha256")
                .isEqualTo(details);
        Assertions.assertNotNull(details.getSha256());
        Assertions.assertTrue(details.isSuccess());
    }

    @Test
    void generateKnownHashesFile_success() {
        String fileName = "KNOWN_HASHES_GEN_LOCAL_date_0.csv";
        BatchResultDetails build = BatchResultDetails.builder()
                .fileName(fileName)
                .success(true)
                .numberOfRows(2)
                .build();
        Instant now = Instant.now();
        BatchResultDetails batchResultDetails = fileGeneratorService.generateKnownHashesFile(now, 0, Arrays.asList("hash", "hash2"));
        Assertions.assertEquals(build, batchResultDetails);
        verify(blobService).uploadFile(any(), eq(now), eq(fileName), eq(null), eq(BatchEnum.KNOWN_HASHES_GEN));
    }

}
