package it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.model.request;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VisaBinRangeRequestData {

    private String binRangeSearchIndex;

    private String binRangeCount;

}
