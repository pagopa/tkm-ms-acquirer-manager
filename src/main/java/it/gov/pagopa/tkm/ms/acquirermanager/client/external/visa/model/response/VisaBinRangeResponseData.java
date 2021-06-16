package it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.model.response;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.*;
import lombok.*;

import java.time.*;

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

    public TkmBinRange toTkmBinRange() {
        return new TkmBinRange()
                .setCircuit(CircuitEnum.VISA)
                .setInsertDate(Instant.now())
                .setMin(binRangeMinNum)
                .setMax(binRangeMaxNum);
    }

}
