package it.gov.pagopa.tkm.ms.acquirermanager.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchResultDetails {

    private String fileName;

    private int fileSize;

    private String sha256;

    private boolean success;

}
