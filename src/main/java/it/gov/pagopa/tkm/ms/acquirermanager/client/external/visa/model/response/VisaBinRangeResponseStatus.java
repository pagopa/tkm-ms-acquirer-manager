package it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.model.response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VisaBinRangeResponseStatus {

    private String statusCode;

    private String statusDescription;

}
