package it.gov.pagopa.tkm.ms.acquirermanager.constant;

import com.azure.storage.blob.models.BlobItem;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.BatchResultDetails;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBatchResult;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBinRange;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.LinksResponse;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BlobMetadataEnum.generationdate;

public class DefaultBeans {

    public static final String TEST_CONNECTION_STRING = "DefaultEndpointsProtocol=https;AccountName=TESTACCOUNTNAME;AccountKey=TESTACCOUNTKEY;BlobEndpoint=https://127.0.0.1:8125/TESTCONTAINER;";

    public static final String TEST_CONTAINER_NAME = "TESTCONTAINERNAME";
    public static final String SHA_256 = "449f8f50cf0d3af36b8dd8538e596902b562681690413148231abf15403207c3";

    public static final Instant INSTANT = Instant.EPOCH;
    public static final OffsetDateTime OFFSET_DATE_TIME = OffsetDateTime.MIN.plus(1000002030, ChronoUnit.YEARS);

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

    public final List<TkmBinRange> TKM_BIN_RANGES = Arrays.asList(
            TkmBinRange.builder().circuit(CircuitEnum.VISA).min("000000000000").max("000000000001").insertDate(INSTANT).build(),
            TkmBinRange.builder().circuit(CircuitEnum.AMEX).min("000000000002").max("000000000003").insertDate(INSTANT).build(),
            TkmBinRange.builder().circuit(CircuitEnum.MASTERCARD).min("000000000004").max("000000000005").insertDate(INSTANT).build()
    );

    public final TkmBatchResult BIN_RANGE_BATCH_RESULT = TkmBatchResult.builder()
            .targetBatch(BatchEnum.BIN_RANGE_GEN)
            .runDate(INSTANT)
            .runDurationMillis(0)
            .runOutcome(true)
            .build();

    public final TkmBatchResult BIN_RANGE_BATCH_RESULT_FAILED = TkmBatchResult.builder()
            .targetBatch(BatchEnum.BIN_RANGE_GEN)
            .runDate(INSTANT)
            .runDurationMillis(0)
            .runOutcome(false)
            .build();

    public final BatchResultDetails BIN_RANGE_BATCH_RESULT_DETAILS = new BatchResultDetails(
            "BIN_RANGE_GEN_LOCAL__1", 3, SHA_256, true, null
    );

    public final BatchResultDetails BIN_RANGE_BATCH_RESULT_DETAILS_EMPTY = new BatchResultDetails(
            "BIN_RANGE_GEN_LOCAL__1", 0, null, true, null
    );

}
