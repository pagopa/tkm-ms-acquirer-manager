package it.gov.pagopa.tkm.ms.acquirermanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.DefaultBeans;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.ReadQueue;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBatchResult;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.BatchResultRepository;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.BatchAcquirerServiceImpl;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.ProducerServiceImpl;
import it.gov.pagopa.tkm.ms.acquirermanager.thread.SendBatchAcquirerRecordToQueue;
import it.gov.pagopa.tkm.ms.acquirermanager.util.ObjectMapperUtils;
import it.gov.pagopa.tkm.ms.acquirermanager.util.PgpUtils;
import it.gov.pagopa.tkm.ms.acquirermanager.util.SftpUtils;
import it.gov.pagopa.tkm.ms.acquirermanager.util.ZipUtils;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.openpgp.PGPException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.DefaultBeans.UUID_TEST;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class TestBatchAcquirerService {

    @InjectMocks
    private BatchAcquirerServiceImpl batchAcquirerService;

    @Mock
    private BatchResultRepository batchResultRepository;

    @Mock
    private SftpUtils sftpUtils;

    @Mock
    private ObjectMapperUtils mapperUtils;

    @Mock
    private Tracer tracer;

    @Mock
    private ProducerServiceImpl producerService;

    @Mock
    private SendBatchAcquirerRecordToQueue sendBatchAcquirerRecordToQueue;


    private ObjectMapper objectMapper = new ObjectMapper();


    private DefaultBeans testBeans = new DefaultBeans();

    private final MockedStatic<Instant> instantMockedStatic = mockStatic(Instant.class);
    private final MockedStatic<ZipUtils> zipUtilsMockedStatic = mockStatic(ZipUtils.class);
    private final MockedStatic<PgpUtils> pgpUtilsMockedStatic = mockStatic(PgpUtils.class);
    private final MockedStatic<UUID> mockedUuid = mockStatic(UUID.class);

    @BeforeEach
    void init() throws IOException {
        instantMockedStatic.when(Instant::now).thenReturn(DefaultBeans.INSTANT);
        zipUtilsMockedStatic.when(()-> ZipUtils.unzipFile(anyString(), anyString())).thenReturn(testBeans.FILE_PATH_LIST);
        mockedUuid.when(UUID::randomUUID).thenReturn(UUID_TEST);
        pgpUtilsMockedStatic.when(() -> PgpUtils.decrypt(anyString(), any(byte[].class), any(char[].class), anyString()))
        .thenAnswer((i) -> {
                    FileUtils.copyFile(new File("src/test/resources/" + UUID_TEST + ".clear"),
                            new File(testBeans.FILE_PATH_LIST.get(0)+".clear"));
                    return null;
                });
        String privateKey = FileUtils.readFileToString(new File("src/test/resources/junit_pgp_private.asc"), StandardCharsets.UTF_8.name());
        ReflectionTestUtils.setField(batchAcquirerService, "pgpPrivateKey", privateKey.getBytes());
        ReflectionTestUtils.setField(batchAcquirerService, "pgpPassPhrase", "passphrase".toCharArray());
    }

    @AfterAll
    void tearDown() {
        instantMockedStatic.close();
    }

    @Test
    void givenRemoteResourceInfo_success() throws IOException {
        doAnswer((i) -> {
            FileUtils.copyFile(new File("src/test/resources/" + UUID_TEST),
                    new File(testBeans.FILE_PATH_LIST.get(0)));
            return null;
        }).when(sftpUtils).downloadFile(anyString(), anyString());
        when(sftpUtils.listFile()).thenReturn(testBeans.REMOTE_RESOURCE_INFO);
        String details = objectMapper.writeValueAsString(Collections.singletonList(testBeans.BATCH_RESULT_DETAILS));
        when(mapperUtils.toJsonOrNull(Collections.singletonList(testBeans.BATCH_RESULT_DETAILS))).thenReturn(details);
        batchAcquirerService.queueBatchAcquirerResult();
        verify(sftpUtils, times(1)).downloadFile(anyString(), anyString());
        testBeans.TKM_BATCH_RESULT_SUCCESS.setDetails(details);
        verify(batchResultRepository).save(testBeans.TKM_BATCH_RESULT_SUCCESS);
    }

    @Test
    void givenException_resultFalse() throws IOException, PGPException {
        doAnswer((i) -> {
            FileUtils.copyFile(new File("src/test/resources/" + UUID_TEST),
                    new File(testBeans.FILE_PATH_LIST.get(0)));
            return null;
        }).when(sftpUtils).downloadFile(anyString(), anyString());
        when(sftpUtils.listFile()).thenReturn(testBeans.REMOTE_RESOURCE_INFO);
        String details = objectMapper.writeValueAsString(Collections.singletonList(testBeans.BATCH_RESULT_DETAILS_UNSUCCESSFUL));
        doThrow(new PGPException("")).when(producerService).sendMessage(any(ReadQueue.class));
        when(mapperUtils.toJsonOrNull(Collections.singletonList(testBeans.BATCH_RESULT_DETAILS_UNSUCCESSFUL))).thenReturn(details);
        batchAcquirerService.queueBatchAcquirerResult();
        testBeans.TKM_BATCH_RESULT_UNSUCCESSFUL.setDetails(details);
        verify(batchResultRepository).save(testBeans.TKM_BATCH_RESULT_UNSUCCESSFUL);
    }


    // testare che scarica il file
    // che scrive result a db

}
