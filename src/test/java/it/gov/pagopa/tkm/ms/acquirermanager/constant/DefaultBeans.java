package it.gov.pagopa.tkm.ms.acquirermanager.constant;

import com.azure.storage.blob.models.*;
import it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.model.response.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.*;

import java.time.*;
import java.time.temporal.*;
import java.util.*;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BlobMetadataEnum.generationdate;

public class DefaultBeans {

    public static final String TEST_CONNECTION_STRING = "DefaultEndpointsProtocol=https;AccountName=TESTACCOUNTNAME;AccountKey=TESTACCOUNTKEY;BlobEndpoint=https://127.0.0.1:8125/TESTCONTAINER;";

    public static final String TEST_CONTAINER_NAME = "TESTCONTAINERNAME";

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

}
