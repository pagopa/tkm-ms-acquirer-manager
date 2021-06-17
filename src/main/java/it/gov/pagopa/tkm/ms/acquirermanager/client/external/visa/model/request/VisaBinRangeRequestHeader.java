package it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.model.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import it.gov.pagopa.tkm.constant.TkmDatetimeConstant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VisaBinRangeRequestHeader {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS", timezone = TkmDatetimeConstant.DATE_TIME_TIMEZONE)
    private Instant requestTS;

    private String requestMessageID;

}
