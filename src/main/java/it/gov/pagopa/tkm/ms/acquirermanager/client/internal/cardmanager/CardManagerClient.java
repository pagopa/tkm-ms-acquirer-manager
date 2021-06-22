package it.gov.pagopa.tkm.ms.acquirermanager.client.internal.cardmanager;

import it.gov.pagopa.tkm.ms.acquirermanager.client.internal.cardmanager.model.response.*;
import org.springframework.cloud.openfeign.*;
import org.springframework.web.bind.annotation.*;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.ApiParams.*;

@FeignClient(value = "card-manager-known-hashes", url = "${client-urls.card-manager}")
public interface CardManagerClient {

    @GetMapping("/known-hashes")
    KnownHashesResponse getKnownHpans(
            @RequestParam(value = MAX_NUMBER_OF_RECORDS_PARAM)
            Integer maxRecords,
            @RequestParam(value = HPAN_OFFSET_PARAM)
            Integer hpanOffset,
            @RequestParam(value = HTOKEN_OFFSET_PARAM)
            Integer htokenOffset
    );

}
