package it.gov.pagopa.tkm.ms.acquirermanager.service;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.*;

public interface FileLinksService {

    LinksResponse getSasLinkResponse(BatchEnum batchEnum);

}