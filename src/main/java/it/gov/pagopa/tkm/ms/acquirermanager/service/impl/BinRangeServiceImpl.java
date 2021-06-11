package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum;
import it.gov.pagopa.tkm.ms.acquirermanager.exception.AcquirerDataNotFoundException;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.LinksResponse;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BinRangeService;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class BinRangeServiceImpl implements BinRangeService {

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${BLOB_STORAGE_BIN_HASH_CONTAINER}")
    private String containerName;

    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("uuuuMMdd").withZone(ZoneId.of("Europe/Rome"));

    private static final String GENERATION_DATE_METADATA = "generationdate";

    @Override
    public LinksResponse getBinRangeFiles() {
        BlobServiceClient serviceClient = new BlobServiceClientBuilder().connectionString(connectionString).buildClient();
        BlobContainerClient client = serviceClient.getBlobContainerClient(containerName);
        PagedIterable<BlobItem> blobItems = getBlobItems(client);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime offsetDateTime = now.plusMinutes(getAvailableFor(blobItems.stream().count()));
        List<String> links = getLinks(offsetDateTime, client, blobItems);
        String genDate = blobItems.stream().findFirst().map(b -> b.getMetadata().get(GENERATION_DATE_METADATA)).orElse(Instant.EPOCH.toString());
        return LinksResponse.builder()
                .fileLinks(links)
                .numberOfFiles(links.size())
                .availableUntil(offsetDateTime.toInstant())
                .generationDate(Instant.parse(genDate))
                .expiredIn(offsetDateTime.toEpochSecond() - now.toEpochSecond())
                .build();
    }

    private PagedIterable<BlobItem> getBlobItems(BlobContainerClient client) {
        String directory = String.format("%s/%s/", BatchEnum.BIN_RANGE_GEN, dateFormat.format(Instant.now()));
        PagedIterable<BlobItem> blobItems = client.listBlobsByHierarchy(directory);
        if (blobItems.stream().count() == 0) {
            throw new AcquirerDataNotFoundException();
        }
        return blobItems;
    }

    private List<String> getLinks(OffsetDateTime expireTime, BlobContainerClient client, PagedIterable<BlobItem> blobItems) {
        BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(expireTime, new BlobContainerSasPermission().setReadPermission(true));
        List<String> links = new ArrayList<>();
        String completeContainerUrl = client.getBlobContainerUrl();
        for (BlobItem blobItem : blobItems) {
            String blobName = blobItem.getName();
            BlobClient blobClient = new BlobClientBuilder()
                    .connectionString(connectionString)
                    .blobName(blobName)
                    .containerName(containerName)
                    .buildClient();
            links.add(String.format("%s/%s?%s", completeContainerUrl, blobName, blobClient.generateSas(sasValues)));
        }
        return links;
    }

    private long getAvailableFor(long linksSize) {
        return NumberUtils.min(10, linksSize * 2);
    }

}
