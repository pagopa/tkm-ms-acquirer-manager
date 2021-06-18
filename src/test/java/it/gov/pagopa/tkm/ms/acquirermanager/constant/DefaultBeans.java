package it.gov.pagopa.tkm.ms.acquirermanager.constant;

import com.azure.storage.blob.models.*;
import it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.model.response.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BlobMetadataEnum.generationdate;

public class DefaultBeans {

    public static final String TEST_CONNECTION_STRING = "DefaultEndpointsProtocol=https;AccountName=TESTACCOUNTNAME;AccountKey=TESTACCOUNTKEY;BlobEndpoint=https://127.0.0.1:8125/TESTCONTAINER;";

    public static final String TEST_CONTAINER_NAME = "TESTCONTAINERNAME";
    private static final String SHA_256 = "449f8f50cf0d3af36b8dd8538e596902b562681690413148231abf15403207c3";

    public static final Instant INSTANT = Instant.EPOCH;
    public static final OffsetDateTime OFFSET_DATE_TIME = OffsetDateTime.MIN.plus(1000002030, ChronoUnit.YEARS);

    private final Map<String, String> GENERATION_DATE_METADATA = new HashMap<>(Collections.singletonMap(generationdate.name(), INSTANT.toString()));

    private final BlobItem BLOB = new BlobItem().setMetadata(GENERATION_DATE_METADATA).setName("TESTNAME");

    public final List<BlobItem> BLOB_LIST = Collections.singletonList(BLOB);
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("uuuuMMdd").withZone(ZoneId.of(TkmDatetimeConstant.DATE_TIME_TIMEZONE));
    public static final String BIN_RANGE_GEN_LOCAL__1 = "BIN_RANGE_GEN_TEST_" + dateFormat.format(Instant.now()) + "_1.csv";

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
            .executionTraceId("traceId")
            .build();

    public final TkmBatchResult BIN_RANGE_BATCH_RESULT_FAILED = TkmBatchResult.builder()
            .targetBatch(BatchEnum.BIN_RANGE_GEN)
            .runDate(INSTANT)
            .runDurationMillis(0)
            .runOutcome(false)
            .build();

    public final BatchResultDetails BIN_RANGE_BATCH_RESULT_DETAILS = new BatchResultDetails(
            BIN_RANGE_GEN_LOCAL__1, 3, SHA_256, true, null
    );

    public final BatchResultDetails BIN_RANGE_BATCH_RESULT_ERROR_DETAILS = new BatchResultDetails(
            BIN_RANGE_GEN_LOCAL__1, 0, null, false, "error"
    );


    public final BatchResultDetails BIN_RANGE_BATCH_RESULT_DETAILS_EMPTY = new BatchResultDetails(
            BIN_RANGE_GEN_LOCAL__1, 0, null, true, null
    );

    private final VisaBinRangeResponseData VISA_BIN_RANGE_RESPONSE_DATA_TOKEN = VisaBinRangeResponseData.builder()
            .binRangeMinNum("0000")
            .binRangeMaxNum("0001")
            .binRangePaymentAccountType("T")
            .build();

    private final VisaBinRangeResponseData VISA_BIN_RANGE_RESPONSE_DATA_PAN = VisaBinRangeResponseData.builder()
            .binRangeMinNum("0002")
            .binRangeMaxNum("0003")
            .binRangePaymentAccountType("P")
            .build();

    private final VisaBinRangeResponseStatus VISA_BIN_RANGE_RESPONSE_STATUS = VisaBinRangeResponseStatus.builder()
            .statusCode("CDI000")
            .build();

    public final VisaBinRangeResponse VISA_BIN_RANGE_RESPONSE = VisaBinRangeResponse.builder()
            .numRecordsReturned("500")
            .areNextOffsetRecordsAvailable("Y")
            .responseData(Arrays.asList(VISA_BIN_RANGE_RESPONSE_DATA_PAN, VISA_BIN_RANGE_RESPONSE_DATA_TOKEN))
            .responseStatus(VISA_BIN_RANGE_RESPONSE_STATUS)
            .totalRecordsCount("1000")
            .build();

    public final VisaBinRangeResponse VISA_BIN_RANGE_RESPONSE_LAST = VisaBinRangeResponse.builder()
            .numRecordsReturned("500")
            .areNextOffsetRecordsAvailable("N")
            .responseData(Arrays.asList(VISA_BIN_RANGE_RESPONSE_DATA_PAN, VISA_BIN_RANGE_RESPONSE_DATA_TOKEN))
            .responseStatus(VISA_BIN_RANGE_RESPONSE_STATUS)
            .totalRecordsCount("1000")
            .build();

    public final List<TkmBinRange> VISA_TKM_BIN_RANGES = Arrays.asList(
            TkmBinRange.builder()
                    .circuit(CircuitEnum.VISA)
                    .min("0000")
                    .max("0001")
                    .insertDate(INSTANT)
                    .build(),
            TkmBinRange.builder()
                    .circuit(CircuitEnum.VISA)
                    .min("0000")
                    .max("0001")
                    .insertDate(INSTANT)
                    .build()
            );

    public final TkmBatchResult VISA_BIN_RANGE_RETRIEVAL_BATCH_RESULT = TkmBatchResult.builder()
            .runOutcome(true)
            .targetBatch(BatchEnum.BIN_RANGE_RETRIEVAL)
            .details("{\"fileSize\":3}")
            .runDate(INSTANT)
            .runDurationMillis(0)
            .build();

    public final TkmBatchResult VISA_BIN_RANGE_RETRIEVAL_BATCH_RESULT_FAILED = TkmBatchResult.builder()
            .runOutcome(false)
            .targetBatch(BatchEnum.BIN_RANGE_RETRIEVAL)
            .details("ERROR RETRIEVING")
            .runDate(INSTANT)
            .runDurationMillis(0)
            .build();

}
