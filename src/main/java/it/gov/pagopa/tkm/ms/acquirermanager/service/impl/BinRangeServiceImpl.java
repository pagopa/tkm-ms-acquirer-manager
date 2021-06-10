package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.common.sas.AccountSasPermission;
import com.azure.storage.common.sas.AccountSasResourceType;
import com.azure.storage.common.sas.AccountSasService;
import com.azure.storage.common.sas.AccountSasSignatureValues;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.LinksResponse;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BinRangeService;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class BinRangeServiceImpl implements BinRangeService {

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${BLOB_STORAGE_BIN_HASH_CONTAINER}")
    private String containerName;

    @Value("${AZURE_KEYVAULT_PROFILE}")
    private String profile;

    private BlobContainerClient client;

    private String sas;

    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("uuuuMMdd").withZone(ZoneId.of("Europe/Rome"));

    @PostConstruct
    public void init() {
        AccountSasPermission permissions = new AccountSasPermission().setListPermission(true).setReadPermission(true);
        AccountSasResourceType resourceTypes = new AccountSasResourceType().setObject(true);
        AccountSasService services = new AccountSasService().setBlobAccess(true);
        OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(10);
        AccountSasSignatureValues sasValues = new AccountSasSignatureValues(expiryTime, permissions, services, resourceTypes);
        BlobServiceClient serviceClient = new BlobServiceClientBuilder().connectionString(connectionString).buildClient();
        sas = serviceClient.generateAccountSas(sasValues);
        client = serviceClient.getBlobContainerClient(containerName);
    }

    @Override
    public LinksResponse getBinRangeFiles() {
        List<String> links = getLinks();
        int linksSize = links.size();
        return new LinksResponse(
                links,
                linksSize,
                getAvailableUntil(linksSize),
                Instant.now()
        );
    }

    private List<String> getLinks() {
        List<String> links = new ArrayList<>();
        String formatNow = dateFormat.format(Instant.now());
        String completeContainerUrl = client.getBlobContainerUrl();
        for (BlobItem blobItem : client.listBlobsByHierarchy(BatchEnum.BIN_RANGE_GEN + "/" + formatNow)) {
            links.add(String.format("%s/%s?%s", completeContainerUrl, blobItem.getName(), sas));
        }
        return links;
    }

    private Instant getAvailableUntil(int linksSize) {
        int duration = NumberUtils.min(10, linksSize * 2);
        return Instant.now().plus(duration, ChronoUnit.MINUTES);
    }

}
