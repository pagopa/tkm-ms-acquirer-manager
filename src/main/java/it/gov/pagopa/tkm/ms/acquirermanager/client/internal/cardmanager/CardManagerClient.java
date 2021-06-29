package it.gov.pagopa.tkm.ms.acquirermanager.client.internal.cardmanager;

import it.gov.pagopa.tkm.ms.acquirermanager.client.internal.cardmanager.model.response.KnownHashesResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.ApiParams.*;

@FeignClient(value = "card-manager-known-hashes", url = "${client-urls.card-manager}")
public interface CardManagerClient {

    @GetMapping("/known-hashes")
    KnownHashesResponse getKnownHashes(
            @RequestParam(value = MAX_NUMBER_OF_RECORDS_PARAM)
                    Integer maxRecords,
            @RequestParam(value = HPAN_OFFSET_PARAM)
                    Integer hpanOffset,
            @RequestParam(value = HTOKEN_OFFSET_PARAM)
                    Integer htokenOffset
    );

}
