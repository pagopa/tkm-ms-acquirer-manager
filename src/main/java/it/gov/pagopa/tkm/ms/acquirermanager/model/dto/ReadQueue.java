package it.gov.pagopa.tkm.ms.acquirermanager.model.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReadQueue {

    private String taxCode;
    private String pan;
    private String hpan;
    private String par;
    private CircuitEnum circuit;
    private List<Token> tokens;

}
