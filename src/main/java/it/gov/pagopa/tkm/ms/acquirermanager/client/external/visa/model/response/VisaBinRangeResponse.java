package it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.model.response;

import lombok.*;

import java.util.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VisaBinRangeResponse {

    private String numRecordsReturned;

    private String areNextOffsetRecordsAvailable;

    private VisaBinRangeResponseHeader responseHeader;

    private List<VisaBinRangeResponseData> responseData;

    private VisaBinRangeResponseStatus responseStatus;

    private String totalRecordsCount;

    public boolean hasMore() {
        return "Y".equals(areNextOffsetRecordsAvailable);
    }

}
