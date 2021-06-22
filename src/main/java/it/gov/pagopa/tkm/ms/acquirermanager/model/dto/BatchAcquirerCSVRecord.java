package it.gov.pagopa.tkm.ms.acquirermanager.model.dto;

import com.univocity.parsers.annotations.Parsed;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchAcquirerCSVRecord {

    @Parsed(index = 0)
    private String token;

    @Parsed(index = 1)
    private CircuitEnum circuit;

    @Parsed(index = 2)
    private String par;

}
