package it.gov.pagopa.tkm.ms.acquirermanager.controller.impl;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum;
import it.gov.pagopa.tkm.ms.acquirermanager.controller.HashingKnownController;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.LinksResponse;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.BinRangeHashingServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HashingKnownControllerImpl implements HashingKnownController {
    @Autowired
    private BinRangeHashingServiceImpl binRangeService;

    @Override
    public LinksResponse getKnownHpanAndHtoken() {
        return binRangeService.getSasLinkResponse(BatchEnum.HTOKEN_HPAN_GEN);
    }
}
