package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import com.azure.core.http.rest.*;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.*;
import com.azure.storage.common.sas.*;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.exception.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.*;
import it.gov.pagopa.tkm.ms.acquirermanager.service.*;
import org.apache.commons.lang3.math.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;

@Service
public class BinRangeServiceImpl implements BinRangeService {

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${BLOB_STORAGE_CONTAINER}")
    private String containerName;

    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("uuuuMMdd").withZone(ZoneId.of("Europe/Rome"));

    @Override
    public LinksResponse getBinRangeFiles() {
        List<String> links = getLinks();
        int linksSize = links.size();
        return new LinksResponse(
                links,
                linksSize,
                Instant.now().plus(getAvailableFor(linksSize), ChronoUnit.MINUTES),
                Instant.now()
        );
    }

    private List<String> getLinks() {
        BlobServiceClient serviceClient = new BlobServiceClientBuilder().connectionString(connectionString).buildClient();
        BlobContainerClient client = serviceClient.getBlobContainerClient(containerName);
        String directory = BatchEnum.BIN_RANGE_GEN + "/" + dateFormat.format(Instant.now()) + "/";
        PagedIterable<BlobItem> blobItems = client.listBlobsByHierarchy(directory);
        long numberOfFiles = blobItems.stream().count();
        if (numberOfFiles == 0) {
            throw new AcquirerDataNotFoundException();
        }
        AccountSasSignatureValues sasValues = new AccountSasSignatureValues(
                OffsetDateTime.now().plusMinutes(getAvailableFor(numberOfFiles)),
                new AccountSasPermission().setReadPermission(true),
                new AccountSasService().setBlobAccess(true),
                new AccountSasResourceType().setObject(true)
        );
        List<String> links = new ArrayList<>();
        String completeContainerUrl = client.getBlobContainerUrl();
        String sas = serviceClient.generateAccountSas(sasValues);
        for (BlobItem blobItem : blobItems) {
            links.add(completeContainerUrl + "/" + blobItem.getName() + "?" + sas);
        }
        return links;
    }

    private long getAvailableFor(long linksSize) {
        return NumberUtils.min(10, linksSize * 2);
    }

}
