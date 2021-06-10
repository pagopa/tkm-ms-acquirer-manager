package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import com.azure.storage.blob.*;
import com.azure.storage.blob.models.*;
import com.azure.storage.common.sas.*;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.*;
import it.gov.pagopa.tkm.ms.acquirermanager.service.*;
import org.apache.commons.lang3.*;
import org.apache.commons.lang3.math.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import javax.annotation.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;

@Service
public class BinRangeServiceImpl implements BinRangeService {

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${BLOB_STORAGE_NAME}")
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
        String directory = StringUtils.joinWith("_",
                BatchEnum.BIN_RANGE_GEN,
                profile,
                dateFormat.format(Instant.now())
        );
        String completeContainerUrl = client.getBlobContainerUrl();
        for (BlobItem blobItem : client.listBlobsByHierarchy(directory + "/")) {
            links.add(completeContainerUrl + "/" + blobItem.getName() + "?" + sas);
        }
        return links;
    }

    private Instant getAvailableUntil(int linksSize) {
        int duration = NumberUtils.min(10, linksSize * 2);
        return Instant.now().plus(duration, ChronoUnit.MINUTES);
    }

}
