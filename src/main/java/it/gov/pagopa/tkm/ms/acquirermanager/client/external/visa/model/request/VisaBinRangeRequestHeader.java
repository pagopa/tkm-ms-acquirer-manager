package it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.model.request;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VisaBinRangeRequestHeader {

    private String requestTS;

    private String requestMessageID;

}
