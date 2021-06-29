package it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.model.request;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VisaBinRangeRequest {

    private VisaBinRangeRequestHeader requestHeader;

    private VisaBinRangeRequestData requestData;

}
