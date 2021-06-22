package it.gov.pagopa.tkm.ms.acquirermanager.model.dto;


import com.univocity.parsers.annotations.Parsed;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadQueue {

    private String taxCode;
    private String pan;
    private String hpan;
    private String par;
    private CircuitEnum circuit;
    private List<Token> tokens;

}
