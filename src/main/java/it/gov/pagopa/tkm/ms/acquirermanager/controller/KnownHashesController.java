package it.gov.pagopa.tkm.ms.acquirermanager.controller;

import it.gov.pagopa.tkm.ms.acquirermanager.model.response.LinksResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.ApiEndpoints.KNOWN_HASHES_BASE_PATH;
import static it.gov.pagopa.tkm.ms.acquirermanager.constant.ApiEndpoints.LINKS;

@RequestMapping(KNOWN_HASHES_BASE_PATH)
public interface KnownHashesController {

    @GetMapping(LINKS)
    LinksResponse getKnownHpanAndHtoken();

}
