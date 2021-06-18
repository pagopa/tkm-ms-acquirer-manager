package it.gov.pagopa.tkm.ms.acquirermanager.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import it.gov.pagopa.tkm.constant.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinksResponse {

    private List<String> fileLinks;

    private Integer numberOfFiles;

    private Long expiredIn;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TkmDatetimeConstant.DATE_TIME_PATTERN, timezone = TkmDatetimeConstant.DATE_TIME_TIMEZONE)
    private Instant availableUntil;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TkmDatetimeConstant.DATE_TIME_PATTERN, timezone = TkmDatetimeConstant.DATE_TIME_TIMEZONE)
    private Instant generationDate;

}
