package it.gov.pagopa.tkm.ms.acquirermanager.service;

import com.azure.storage.blob.*;
import com.azure.storage.blob.specialized.*;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.DefaultBeans;
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
import java.time.format.DateTimeFormatter;

import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
public class TestBlobService {

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

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(blobService, "containerNameBinHash", DefaultBeans.TEST_CONTAINER_NAME);
        ReflectionTestUtils.setField(blobService, "connectionString", DefaultBeans.TEST_CONNECTION_STRING);
        ReflectionTestUtils.setField(blobService, "profile", "local");
        ReflectionTestUtils.setField(blobService, "serviceClientBuilder", serviceClientBuilderMock);
        when(serviceClientBuilderMock.connectionString(DefaultBeans.TEST_CONNECTION_STRING)).thenReturn(serviceClientBuilderMock);
        when(serviceClientBuilderMock.buildClient()).thenReturn(serviceClientMock);
        when(serviceClientMock.getBlobContainerClient(DefaultBeans.TEST_CONTAINER_NAME)).thenReturn(containerClientMock);
        when(containerClientMock.getBlobClient(any())).thenReturn(blobClientMock);
    }

    @Test
    void givenFile_uploadBinRangesFile() {
        blobService.uploadFile(new byte[]{}, DefaultBeans.INSTANT, "filename", "sha", BatchEnum.BIN_RANGE_GEN);
        verify(blobClientMock).upload(any(InputStream.class), anyLong(), anyBoolean());
        verify(blobClientMock).setMetadata(anyMap());
    }

    @Test
    void givenFile_uploadKnownHashesFile() {
        when(blobClientMock.getAppendBlobClient()).thenReturn(appendBlobClientMock);
        blobService.uploadFile(new byte[]{}, DefaultBeans.INSTANT, "filename", "sha", BatchEnum.KNOWN_HASHES_GEN);
        verify(appendBlobClientMock).create();
        verify(appendBlobClientMock).appendBlock(any(InputStream.class), anyLong());
        verify(blobClientMock).setMetadata(anyMap());
    }

}
