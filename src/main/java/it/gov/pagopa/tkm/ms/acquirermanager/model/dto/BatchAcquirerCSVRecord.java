package it.gov.pagopa.tkm.ms.acquirermanager.model.dto;

import com.univocity.parsers.annotations.EnumOptions;
import com.univocity.parsers.annotations.Parsed;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BatchAcquirerCSVRecord {

    @Parsed(index = 0)
    private String token;

    @EnumOptions(customElement = "fromCodeWithDefault")
    @Parsed(index = 1, defaultNullRead = "UNKNOWN")
    private CircuitEnum circuit;

    @Parsed(index = 2)
    private String par;

}
