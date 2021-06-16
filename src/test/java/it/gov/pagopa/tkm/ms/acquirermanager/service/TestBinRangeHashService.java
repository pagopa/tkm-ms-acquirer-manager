package it.gov.pagopa.tkm.ms.acquirermanager.service;

import com.azure.core.http.rest.*;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.exception.*;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.*;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.test.util.*;

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
public class TestBinRangeHashService {

    @InjectMocks
    private BinRangeHashServiceImpl binRangeHashService;

    @Mock
    private BlobServiceClientBuilder serviceClientBuilderMock;

    @Mock
    private BlobClientBuilder blobClientBuilderMock;

    @Mock
    private BlobClient blobClientMock;

    @Mock
    private BlobServiceClient serviceClientMock;

    @Mock
    private BlobContainerClient containerClientMock;

    @Mock
    private PagedIterable<BlobItem> pagedIterableMock;

    @Mock
    private DateTimeFormatter dateFormatMock;

    @Mock
    private BinRangeRepository binRangeRepository;

    @Mock
    private BatchResultRepository batchResultRepository;

    @Mock
    private ObjectMapper mapper;

    private DefaultBeans testBeans;

    private final MockedStatic<Instant> instantMockedStatic = mockStatic(Instant.class);
    private final MockedStatic<OffsetDateTime> offsetMockedStatic = mockStatic(OffsetDateTime.class);

    @BeforeEach
    void init() {
        testBeans = new DefaultBeans();
        instantMockedStatic.when(Instant::now).thenReturn(DefaultBeans.INSTANT);
        offsetMockedStatic.when(OffsetDateTime::now).thenReturn(DefaultBeans.OFFSET_DATE_TIME);
        ReflectionTestUtils.setField(binRangeHashService, "connectionString", DefaultBeans.TEST_CONNECTION_STRING);
        ReflectionTestUtils.setField(binRangeHashService, "containerName", DefaultBeans.TEST_CONTAINER_NAME);
        ReflectionTestUtils.setField(binRangeHashService, "dateFormat", dateFormatMock);
        ReflectionTestUtils.setField(binRangeHashService, "serviceClientBuilder", serviceClientBuilderMock);
        ReflectionTestUtils.setField(binRangeHashService, "blobClientBuilder", blobClientBuilderMock);
        ReflectionTestUtils.setField(binRangeHashService, "maxRowsInFiles", 2);
        ReflectionTestUtils.setField(binRangeHashService, "profile", "local");
    }

    @AfterAll
    void tearDown() {
        instantMockedStatic.close();
        offsetMockedStatic.close();
    }

    private void startupAssumptions(boolean shouldThrowException) {
        when(serviceClientBuilderMock.connectionString(DefaultBeans.TEST_CONNECTION_STRING)).thenReturn(serviceClientBuilderMock);
        when(serviceClientBuilderMock.buildClient()).thenReturn(serviceClientMock);
        if (!shouldThrowException) {
            when(blobClientBuilderMock.connectionString(DefaultBeans.TEST_CONNECTION_STRING)).thenReturn(blobClientBuilderMock);
            when(blobClientBuilderMock.containerName(DefaultBeans.TEST_CONTAINER_NAME)).thenReturn(blobClientBuilderMock);
            when(blobClientBuilderMock.blobName(notNull())).thenReturn(blobClientBuilderMock);
            when(blobClientBuilderMock.buildClient()).thenReturn(blobClientMock);
        }
        when(serviceClientMock.getBlobContainerClient(DefaultBeans.TEST_CONTAINER_NAME)).thenReturn(containerClientMock);
        when(containerClientMock.listBlobsByHierarchy(nullable(String.class), nullable(ListBlobsOptions.class), nullable(Duration.class))).thenReturn(pagedIterableMock);
        when(dateFormatMock.format(DefaultBeans.INSTANT)).thenReturn("20210101");
    }

    @Test
    void givenExistingFile_returnLinksResponseBinRanges() {
        startupAssumptions(false);
        when(pagedIterableMock.iterator()).thenReturn(testBeans.BLOB_LIST.iterator());
        assertEquals(testBeans.LINKS_RESPONSE, binRangeHashService.getSasLinkResponse(BatchEnum.BIN_RANGE_GEN));
    }

    @Test
    void givenExistingFile_returnLinksResponseHashes() {
        startupAssumptions(false);
        when(pagedIterableMock.iterator()).thenReturn(testBeans.BLOB_LIST.iterator());
        assertEquals(testBeans.LINKS_RESPONSE, binRangeHashService.getSasLinkResponse(BatchEnum.HTOKEN_HPAN_GEN));
    }

    @Test
    void givenNoFiles_expectNotFoundException() {
        startupAssumptions(true);
        when(pagedIterableMock.iterator()).thenReturn(Collections.emptyIterator());
        assertThrows(AcquirerDataNotFoundException.class, () -> binRangeHashService.getSasLinkResponse(BatchEnum.BIN_RANGE_GEN));
        assertThrows(AcquirerDataNotFoundException.class, () -> binRangeHashService.getSasLinkResponse(BatchEnum.HTOKEN_HPAN_GEN));
    }

    @Test
    void givenBinRanges_generateAndUploadFiles() throws JsonProcessingException {
        when(serviceClientBuilderMock.connectionString(DefaultBeans.TEST_CONNECTION_STRING)).thenReturn(serviceClientBuilderMock);
        when(serviceClientBuilderMock.buildClient()).thenReturn(serviceClientMock);
        when(serviceClientMock.getBlobContainerClient(DefaultBeans.TEST_CONTAINER_NAME)).thenReturn(containerClientMock);
        when(containerClientMock.getBlobClient(any())).thenReturn(blobClientMock);
        when(binRangeRepository.findAll()).thenReturn(testBeans.TKM_BIN_RANGES);
        binRangeHashService.generateBinRangeFiles();
        verify(containerClientMock, times(2)).getBlobClient(anyString());
        verify(blobClientMock, times(2)).upload(any(InputStream.class), anyLong(), anyBoolean());
        verify(blobClientMock, times(2)).setMetadata(anyMap());
        verify(batchResultRepository).saveAll(any());
    }

    @Test
    void givenNoBinRanges_generateAndUploadEmptyFile() throws JsonProcessingException {
        when(serviceClientBuilderMock.connectionString(DefaultBeans.TEST_CONNECTION_STRING)).thenReturn(serviceClientBuilderMock);
        when(serviceClientBuilderMock.buildClient()).thenReturn(serviceClientMock);
        when(serviceClientMock.getBlobContainerClient(DefaultBeans.TEST_CONTAINER_NAME)).thenReturn(containerClientMock);
        when(containerClientMock.getBlobClient(any())).thenReturn(blobClientMock);
        when(binRangeRepository.findAll()).thenReturn(null);
        binRangeHashService.generateBinRangeFiles();
        verify(containerClientMock).getBlobClient(anyString());
        verify(blobClientMock).upload(any(InputStream.class), anyLong(), anyBoolean());
        verify(blobClientMock).setMetadata(anyMap());
        verify(batchResultRepository).saveAll(any());
    }

}
