package it.gov.pagopa.tkm.ms.acquirermanager.service;

import com.azure.storage.blob.*;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.test.util.*;

import java.io.*;
import java.time.format.*;

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
    private DateTimeFormatter dateFormatMock;

    private DefaultBeans testBeans;

    @BeforeEach
    void init() {
        testBeans = new DefaultBeans();
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
        blobService.uploadAcquirerFile(new byte[]{}, DefaultBeans.INSTANT, "filename", "sha");
        verify(blobClientMock).upload(any(InputStream.class), anyLong(), anyBoolean());
        verify(blobClientMock).setMetadata(anyMap());
    }

}
