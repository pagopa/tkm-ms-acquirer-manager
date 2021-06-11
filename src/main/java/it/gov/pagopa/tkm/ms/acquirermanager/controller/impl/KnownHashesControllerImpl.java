package it.gov.pagopa.tkm.ms.acquirermanager.controller.impl;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum;
import it.gov.pagopa.tkm.ms.acquirermanager.controller.KnownHashesController;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.LinksResponse;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.BinRangeHashServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KnownHashesControllerImpl implements KnownHashesController {
    @Autowired
    private BinRangeHashServiceImpl binRangeService;

    @Override
    public LinksResponse getKnownHpanAndHtoken() {
        return binRangeService.getSasLinkResponse(BatchEnum.HTOKEN_HPAN_GEN);
    }
}
