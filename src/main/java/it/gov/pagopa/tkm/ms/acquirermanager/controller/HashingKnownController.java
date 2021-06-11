package it.gov.pagopa.tkm.ms.acquirermanager.controller;

import it.gov.pagopa.tkm.ms.acquirermanager.model.response.LinksResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.ApiEndpoints.HTOKEN_HPAN_BASE_PATH;
import static it.gov.pagopa.tkm.ms.acquirermanager.constant.ApiEndpoints.LINK;

@RequestMapping(HTOKEN_HPAN_BASE_PATH)
public interface HashingKnownController {

    @GetMapping(LINK)
    LinksResponse getKnownHpanAndHtoken();

}
