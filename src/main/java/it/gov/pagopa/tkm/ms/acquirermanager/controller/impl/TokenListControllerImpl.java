package it.gov.pagopa.tkm.ms.acquirermanager.controller.impl;

import com.azure.storage.blob.*;
import com.azure.storage.blob.models.*;
import it.gov.pagopa.tkm.ms.acquirermanager.controller.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.*;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.*;
import lombok.extern.log4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;

import java.io.*;
import java.time.*;
import java.util.*;

@RestController
@Log4j2
public class TokenListControllerImpl implements TokenListController {

    @Value("${BLOB_STORAGE_ACQUIRER_CONFIG_CONTAINER}")
    private String acquirerConfigContainer;

    @Value("${BLOB_STORAGE_ACQUIRER_CONTAINER}")
    private String containerNameAcquirer;

    @Value("${ACQUIRER_FILE_UPLOAD_CHUNK_SIZE_MB}")
    private Long chunkSize;

    @Value("${ACQUIRER_FILE_UPLOAD_MAX_CONCURRENCY}")
    private Integer maxConcurrency;

    @Value("${ACQUIRER_FILE_UPLOAD_TIME_LIMIT_MINUTES}")
    private Long timeLimit;

    @Autowired
    private BlobServiceImpl blobService;

    @Override
    public String getPublicPgpKey() {
        BlobContainerClient client = blobService.getBlobContainerClient(acquirerConfigContainer);
        BlobClient blobClient = client.getBlobClient("acquirer-pgp-pub-key.asc");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blobClient.download(outputStream);
        return outputStream.toString();
    }

    @Override
    public TokenListUploadResponse uploadFile(MultipartFile file) throws IOException {
        String newFilename = UUID.randomUUID() + file.getOriginalFilename();
        BlobContainerClient client = blobService.getBlobContainerClient(containerNameAcquirer);
        BlobClient blobClient = client.getBlobClient(newFilename);
        ParallelTransferOptions options = new ParallelTransferOptions()
                .setBlockSizeLong(chunkSize * 1048576L)
                .setMaxConcurrency(maxConcurrency)
                .setProgressReceiver(bytesTransferred -> log.info("Uploaded " + bytesTransferred + " bytes of " + file.getSize()));
        blobClient.uploadWithResponse(file.getInputStream(), file.getSize(), options, new BlobHttpHeaders().setContentType("binary"), null, AccessTier.HOT, new BlobRequestConditions(), Duration.ofMinutes(timeLimit), null);
        return new TokenListUploadResponse(newFilename);
    }

}
