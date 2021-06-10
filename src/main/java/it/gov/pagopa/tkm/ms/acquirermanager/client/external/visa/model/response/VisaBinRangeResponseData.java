package it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.model.response;

import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VisaBinRangeResponseData {

    private String binRangeMinNum;

    private String binRangeMaxNum;

    private String binRangePaymentAccountType;

    private String productID;

    private String productIDName;

    private String accountFundingSourceCd;

    private String platformCd;

    private String accountRegionCode;

    private String issuerBin;

    private String issuerBillingCurrCd;

    private String accountCtryAlpha2Code;

    public BinRangeDto toBinRangeDto() {
        return new BinRangeDto(
                binRangeMinNum,
                binRangeMaxNum
        );
    }

}
