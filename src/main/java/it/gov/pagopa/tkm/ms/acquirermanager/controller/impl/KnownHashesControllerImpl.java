package it.gov.pagopa.tkm.ms.acquirermanager.controller.impl;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum;
import it.gov.pagopa.tkm.ms.acquirermanager.controller.KnownHashesController;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.LinksResponse;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KnownHashesControllerImpl implements KnownHashesController {

    @Autowired
    private FileLinksServiceImpl fileLinksService;

    @Override
    public LinksResponse getKnownHpanAndHtoken() {
        return fileLinksService.getSasLinkResponse(BatchEnum.KNOWN_HASHES_COPY);
    }

}
