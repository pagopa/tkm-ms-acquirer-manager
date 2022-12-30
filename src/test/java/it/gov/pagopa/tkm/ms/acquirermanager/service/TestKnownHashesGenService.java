package it.gov.pagopa.tkm.ms.acquirermanager.service;

import com.azure.core.http.rest.*;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.*;
import com.azure.storage.blob.specialized.*;
import com.fasterxml.jackson.databind.*;
import it.gov.pagopa.tkm.ms.acquirermanager.client.internal.cardmanager.*;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.*;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.*;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.cloud.sleuth.*;
import org.springframework.test.util.*;

import java.io.*;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class TestKnownHashesGenService {

    @InjectMocks
    private KnownHashesGenServiceImpl knownHashesService;

    @Mock
    private BatchResultRepository batchResultRepository;

    @Mock
    private HashOffsetRepository hashOffsetRepository;

    @Spy
    private ObjectMapper mapper;

    @Mock
    private FileGeneratorServiceImpl fileGeneratorService;

    @Mock
    private Tracer tracer;

    @Mock
    private CardManagerClient cardManagerClient;

    @Mock
    private BlobServiceClientBuilder serviceClientBuilder;

    @Mock
    private BlobServiceClient serviceClient;

    @Mock
    private BlobContainerClient containerClient;

    @Mock
    private PagedIterable<BlobItem> pagedIterable;

    @Mock
    private BlobClient blobClient;

    @Mock
    private AppendBlobClient appendBlobClient;

    @Mock
    private BlobServiceImpl blobService;

    private final DefaultBeans testBeans = new DefaultBeans();

    private final ArgumentCaptor<TkmBatchResult> batchResultArgumentCaptor = ArgumentCaptor.forClass(TkmBatchResult.class);

    private final MockedStatic<Instant> instantMockedStatic = mockStatic(Instant.class);

    @BeforeEach
    void init() {
        instantMockedStatic.when(Instant::now).thenReturn(DefaultBeans.INSTANT);
        ReflectionTestUtils.setField(knownHashesService, "maxRowsInFiles", 4);
        ReflectionTestUtils.setField(knownHashesService, "maxRecordsInApiCall", 4);
        ReflectionTestUtils.setField(knownHashesService, "serviceClientBuilder", serviceClientBuilder);
        ReflectionTestUtils.setField(knownHashesService, "connectionString", DefaultBeans.TEST_CONNECTION_STRING);
        ReflectionTestUtils.setField(knownHashesService, "containerName", DefaultBeans.TEST_CONTAINER_NAME);
    }

    @AfterAll
    void tearDown() {
        instantMockedStatic.close();
    }

    @Test
    void givenKnownHashesAndNoExistingFiles_persistResult() {
        when(cardManagerClient.getKnownHashes(anyInt(), anyInt(), anyInt())).thenReturn(testBeans.KNOWN_HASHES_RESPONSE);
        when(fileGeneratorService.generateKnownHashesFile(any(Instant.class), anyInt(), anyList())).thenReturn(testBeans.KNOWN_HASHES_BATCH_RESULT_DETAILS);
        knownHashesService.generateKnownHashesFiles();
        verify(batchResultRepository).save(batchResultArgumentCaptor.capture());
        assertThat(batchResultArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("details", "executionTraceId")
                .isEqualTo(testBeans.KNOWN_HASHES_GEN_BATCH_RESULT);
    }

    @Test
    void givenKnownHashesAndExistingFile_persistResult() {
        when(hashOffsetRepository.findAll()).thenReturn(Collections.singletonList(testBeans.HASH_OFFSET));
        when(cardManagerClient.getKnownHashes(anyInt(), anyInt(), anyInt())).thenReturn(testBeans.KNOWN_HASHES_RESPONSE);
        when(serviceClientBuilder.connectionString(DefaultBeans.TEST_CONNECTION_STRING)).thenReturn(serviceClientBuilder);
        when(serviceClientBuilder.buildClient()).thenReturn(serviceClient);
        when(serviceClient.getBlobContainerClient(DefaultBeans.TEST_CONTAINER_NAME)).thenReturn(containerClient);
        when(containerClient.listBlobsByHierarchy(nullable(String.class), nullable(ListBlobsOptions.class), nullable(Duration.class))).thenReturn(pagedIterable);
        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(pagedIterable.iterator()).thenReturn(testBeans.BLOB_LIST.iterator());
        when(blobClient.getAppendBlobClient()).thenReturn(appendBlobClient);
        when(appendBlobClient.appendBlock(any(ByteArrayInputStream.class), anyLong())).thenReturn(null);
        knownHashesService.generateKnownHashesFiles();
        verify(batchResultRepository).save(batchResultArgumentCaptor.capture());
        assertThat(batchResultArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("details", "executionTraceId")
                .isEqualTo(testBeans.KNOWN_HASHES_GEN_BATCH_RESULT);
    }

    @Test
    void givenException_persistFalseResult() {
        when(cardManagerClient.getKnownHashes(anyInt(), anyInt(), anyInt())).thenReturn(testBeans.KNOWN_HASHES_RESPONSE);
        when(fileGeneratorService.generateKnownHashesFile(any(Instant.class), anyInt(), anyList())).thenThrow(new RuntimeException());
        knownHashesService.generateKnownHashesFiles();
        verify(batchResultRepository).save(batchResultArgumentCaptor.capture());
        assertThat(batchResultArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("details", "executionTraceId")
                .isEqualTo(testBeans.KNOWN_HASHES_GEN_BATCH_RESULT_FAILED);
    }

}
