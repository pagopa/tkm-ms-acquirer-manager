package it.gov.pagopa.tkm.ms.acquirermanager.model.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchResultDetails {

    private String fileName;

    private int fileSize;

    private String sha256;

}
