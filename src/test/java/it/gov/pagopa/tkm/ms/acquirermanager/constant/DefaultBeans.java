package it.gov.pagopa.tkm.ms.acquirermanager.constant;

import com.azure.storage.blob.models.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.*;

import java.time.*;
import java.time.temporal.*;
import java.util.*;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BlobMetadataEnum.generationdate;

public class DefaultBeans {

    public static final String TEST_CONNECTION_STRING = "DefaultEndpointsProtocol=https;AccountName=TESTACCOUNTNAME;AccountKey=TESTACCOUNTKEY;BlobEndpoint=https://127.0.0.1:8125/TESTCONTAINER;";

    public static final String TEST_CONTAINER_NAME = "TESTCONTAINERNAME";

    public final static Instant INSTANT = Instant.EPOCH;
    public final static OffsetDateTime OFFSET_DATE_TIME = OffsetDateTime.MIN.plus(1000002030, ChronoUnit.YEARS);

    private final Map<String, String> GENERATION_DATE_METADATA = new HashMap<>(Collections.singletonMap(generationdate.name(), INSTANT.toString()));

    public final BlobItem BLOB = new BlobItem().setMetadata(GENERATION_DATE_METADATA).setName("TESTNAME");

    public final List<BlobItem> BLOB_LIST = Collections.singletonList(BLOB);

    public final LinksResponse LINKS_RESPONSE = LinksResponse.builder()
            .fileLinks(Collections.singletonList("null/TESTNAME?null"))
            .numberOfFiles(1)
            .availableUntil(null)
            .generationDate(null)
            .expiredIn(120L)
            .build();

}
