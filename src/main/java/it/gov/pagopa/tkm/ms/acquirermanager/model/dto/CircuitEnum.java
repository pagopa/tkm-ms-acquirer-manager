package it.gov.pagopa.tkm.ms.acquirermanager.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@AllArgsConstructor
public enum CircuitEnum {
    VISA("01"),
    MASTERCARD("02"),
    AMEX("03"),
    JCB("04"),
    UNION_PAY("05"),
    DINERS("06"),
    POSTE_PAY("07"),
    BANCOMAT_PAY("08"),
    SATISPAY("09"),
    PRIVATE("10"),
    UNKNOWN("-1");

    @Getter
    private String code;

    public static CircuitEnum fromCodeWithDefault(String code) {
        return Arrays.stream(values()).filter(v -> v.code.equals(code)).findAny().orElse(UNKNOWN);
    }

    public boolean isAllowedValue() {
        return this == VISA || this == MASTERCARD || this == AMEX;
    }


}
