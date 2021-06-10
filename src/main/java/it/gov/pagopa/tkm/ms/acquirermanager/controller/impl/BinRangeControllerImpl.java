package it.gov.pagopa.tkm.ms.acquirermanager.controller.impl;

import it.gov.pagopa.tkm.ms.acquirermanager.controller.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.*;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class BinRangeControllerImpl implements BinRangeController {

    @Autowired
    private BinRangeServiceImpl binRangeService;

    @Override
    public LinksResponse getBinRangeFiles() {
        return binRangeService.getBinRangeFiles();
    }

}
