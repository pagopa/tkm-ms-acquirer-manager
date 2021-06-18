package it.gov.pagopa.tkm.ms.acquirermanager.model.dto;

import com.fasterxml.jackson.annotation.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchResultDetails {

    private String fileName;

    private int fileSize;

    private String sha256;

}