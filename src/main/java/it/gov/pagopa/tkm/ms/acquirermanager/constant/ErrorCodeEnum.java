package it.gov.pagopa.tkm.ms.acquirermanager.constant;

import com.fasterxml.jackson.annotation.*;
import lombok.*;

@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ErrorCodeEnum {

    CALL_TO_CARD_MANAGER_FAILED(4000, "Could not execute call to Card Manager"),
    DATA_NOT_FOUND(4001, "Requested data could not be found");

    @Getter
    private final Integer statusCode;

    @Getter
    private final String message;

}
