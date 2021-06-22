package it.gov.pagopa.tkm.ms.acquirermanager.client.internal.cardmanager.model.response;

import lombok.*;

import java.util.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnownHashesResponse {

    private List<String> hpans;

    private List<String> htokens;

}
