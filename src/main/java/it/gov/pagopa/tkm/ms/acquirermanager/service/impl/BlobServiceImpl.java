package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import com.azure.storage.blob.*;
import it.gov.pagopa.tkm.constant.TkmDatetimeConstant;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BlobService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum.BIN_RANGE_GEN;
import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BlobMetadataEnum.checksumsha256;
import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BlobMetadataEnum.generationdate;

@Service
@Log4j2
public class BlobServiceImpl implements BlobService {
    private final BlobServiceClientBuilder serviceClientBuilder = new BlobServiceClientBuilder();
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("uuuuMMdd").withZone(ZoneId.of(TkmDatetimeConstant.DATE_TIME_TIMEZONE));

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${BLOB_STORAGE_BIN_HASH_CONTAINER}")
    private String containerNameBinHash;

    @Value("${AZURE_KEYVAULT_PROFILE}")
    private String profile;

    @Override
    public void uploadAcquirerFile(byte[] fileByte, Instant instant, int index) {
        String today = dateFormat.format(instant);
        String filename = StringUtils.joinWith("_", BIN_RANGE_GEN, profile.toUpperCase(), today, index);
        String directory = String.format("%s/%s/", BIN_RANGE_GEN, today);
        BlobServiceClient serviceClient = serviceClientBuilder.connectionString(connectionString).buildClient();
        BlobContainerClient client = serviceClient.getBlobContainerClient(containerNameBinHash);
        BlobClient blobClient = client.getBlobClient(directory + filename + ".zip");
        blobClient.upload(new ByteArrayInputStream(fileByte), fileByte.length, false);
        String sha256 = DigestUtils.sha256Hex(fileByte);
        Map<String, String> metadata = new HashMap<>();
        metadata.put(generationdate.name(), instant.toString());
        metadata.put(checksumsha256.name(), sha256);
        blobClient.setMetadata(metadata);
        log.debug("Uploaded: " + filename);
    }
}
