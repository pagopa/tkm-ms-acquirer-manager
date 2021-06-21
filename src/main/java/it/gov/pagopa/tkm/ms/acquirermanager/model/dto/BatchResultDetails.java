package it.gov.pagopa.tkm.ms.acquirermanager.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class BatchResultDetails {

    private String fileName;

    private Integer numberOfRows;

    private String sha256;

    private boolean success;

    private String errorMessage;

}
