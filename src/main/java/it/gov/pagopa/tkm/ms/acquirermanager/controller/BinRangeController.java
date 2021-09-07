package it.gov.pagopa.tkm.ms.acquirermanager.controller;

import it.gov.pagopa.tkm.ms.acquirermanager.model.response.*;
import org.springframework.web.bind.annotation.*;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.ApiEndpoints.BIN_RANGES_BASE_PATH;
import static it.gov.pagopa.tkm.ms.acquirermanager.constant.ApiEndpoints.LINKS;

@RequestMapping(BIN_RANGES_BASE_PATH)
public interface BinRangeController {

    @GetMapping(LINKS)
    LinksResponse getBinRangeFiles();

}
