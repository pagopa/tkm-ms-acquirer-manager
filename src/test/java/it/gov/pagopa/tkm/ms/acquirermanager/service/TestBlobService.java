package it.gov.pagopa.tkm.ms.acquirermanager.service;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.specialized.AppendBlobClient;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.BlobServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class TestBlobService {

    @InjectMocks
    private BlobServiceImpl blobService;

    @Mock
    private BlobServiceClientBuilder serviceClientBuilderMock;

    @Mock
    private BlobServiceClient serviceClientMock;

    @Mock
    private BlobContainerClient containerClientMock;

    @Mock
    private BlobClientBuilder blobClientBuilderMock;

    @Mock
    private BlobClient blobClientMock;

    @Mock
    private AppendBlobClient appendBlobClientMock;

    @Mock
    private DateTimeFormatter dateFormatMock;

    @Mock
    private PagedIterable<BlobItem> pagedIterable;

    private final DefaultBeans defaultBeans = new DefaultBeans();

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(blobService, "containerNameBinHash", DefaultBeans.TEST_CONTAINER_NAME);
        ReflectionTestUtils.setField(blobService, "connectionString", DefaultBeans.TEST_CONNECTION_STRING);
        ReflectionTestUtils.setField(blobService, "profile", "local");
        ReflectionTestUtils.setField(blobService, "serviceClientBuilder", serviceClientBuilderMock);
    }

    @Test
    void givenFile_uploadFile() {
        when(serviceClientBuilderMock.connectionString(DefaultBeans.TEST_CONNECTION_STRING)).thenReturn(serviceClientBuilderMock);
        when(serviceClientBuilderMock.buildClient()).thenReturn(serviceClientMock);
        when(serviceClientMock.getBlobContainerClient(DefaultBeans.TEST_CONTAINER_NAME)).thenReturn(containerClientMock);
        when(containerClientMock.getBlobClient(any())).thenReturn(blobClientMock);
        when(blobClientMock.getAppendBlobClient()).thenReturn(appendBlobClientMock);
        blobService.uploadFile(new byte[]{}, DefaultBeans.INSTANT, "filename", "sha", BatchEnum.BIN_RANGE_GEN);
        verify(blobClientMock).upload(any(InputStream.class), anyLong(), anyBoolean());
        verify(blobClientMock).setMetadata(anyMap());
        blobService.uploadFile(new byte[]{}, DefaultBeans.INSTANT, "filename", "sha", BatchEnum.KNOWN_HASHES_GEN);
        verify(blobClientMock).upload(any(InputStream.class), anyLong(), anyBoolean());
        verify(blobClientMock).setMetadata(anyMap());
    }

    @Test
    void downloadFileHashingTmp_success() {
        when(serviceClientBuilderMock.connectionString(DefaultBeans.TEST_CONNECTION_STRING)).thenReturn(serviceClientBuilderMock);
        when(serviceClientBuilderMock.buildClient()).thenReturn(serviceClientMock);
        when(serviceClientMock.getBlobContainerClient(DefaultBeans.TEST_CONTAINER_NAME)).thenReturn(containerClientMock);
        when(containerClientMock.getBlobClient(any())).thenReturn(blobClientMock);
        String localPath = "localPath";
        blobService.downloadFileHashingTmp("remotePath", localPath);
        verify(blobClientMock).downloadToFile(eq(localPath), eq(true));
    }

    @Test
    void getBlobItemsInFolderHashingTmp_success() {
        when(serviceClientBuilderMock.connectionString(DefaultBeans.TEST_CONNECTION_STRING)).thenReturn(serviceClientBuilderMock);
        when(serviceClientBuilderMock.buildClient()).thenReturn(serviceClientMock);
        when(serviceClientMock.getBlobContainerClient(DefaultBeans.TEST_CONTAINER_NAME)).thenReturn(containerClientMock);
        when(containerClientMock.listBlobsByHierarchy(anyString(), any(ListBlobsOptions.class), any())).thenReturn(pagedIterable);
        when(pagedIterable.iterator()).thenReturn(defaultBeans.BLOB_LIST.iterator());

        List<BlobItem> items = blobService.getFilesFromDirectory("items");
        assertIterableEquals(defaultBeans.BLOB_LIST, items);
    }

    @Test
    void deleteTodayFolder_success() {
        when(serviceClientBuilderMock.connectionString(DefaultBeans.TEST_CONNECTION_STRING)).thenReturn(serviceClientBuilderMock);
        when(serviceClientBuilderMock.buildClient()).thenReturn(serviceClientMock);
        when(serviceClientMock.getBlobContainerClient(DefaultBeans.TEST_CONTAINER_NAME)).thenReturn(containerClientMock);
        when(containerClientMock.listBlobsByHierarchy(anyString(), any(ListBlobsOptions.class), any())).thenReturn(pagedIterable);
        when(pagedIterable.iterator()).thenReturn(defaultBeans.BLOB_LIST.iterator());
        when(containerClientMock.getBlobClient(any())).thenReturn(blobClientMock);
        blobService.deleteFolder(blobService.getDirectoryName(Instant.now(), BatchEnum.BIN_RANGE_GEN));
        verify(blobClientMock).delete();
    }

    @Test
    void givenBatch_getDirectoryName() {
        assertEquals(
                String.format("%s/%s/", DirectoryNames.KNOWN_HASHES, "19700101"),
                blobService.getDirectoryName(DefaultBeans.INSTANT, BatchEnum.KNOWN_HASHES_COPY));
        assertEquals(
                String.format("%s/%s/", DirectoryNames.KNOWN_HASHES, DirectoryNames.ALL_KNOWN_HASHES),
                blobService.getDirectoryName(DefaultBeans.INSTANT, BatchEnum.KNOWN_HASHES_GEN));
        assertEquals(
                String.format("%s/%s/", DirectoryNames.BIN_RANGES, "19700101"),
                blobService.getDirectoryName(DefaultBeans.INSTANT, BatchEnum.BIN_RANGE_GEN));
        assertNull(blobService.getDirectoryName(DefaultBeans.INSTANT, BatchEnum.BATCH_ACQUIRER));
    }

}
