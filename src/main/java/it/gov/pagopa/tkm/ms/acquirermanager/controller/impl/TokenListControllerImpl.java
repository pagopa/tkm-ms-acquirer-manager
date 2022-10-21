package it.gov.pagopa.tkm.ms.acquirermanager.controller.impl;

import it.gov.pagopa.tkm.ms.acquirermanager.controller.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.*;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;

import java.io.*;

@RestController
public class TokenListControllerImpl implements TokenListController {

    @Autowired
    private BatchAcquirerServiceImpl batchAcquirerService;

    @Override
    public String getPublicPgpKey() {
        return batchAcquirerService.getPublicPgpKey();
    }

    @Override
    public TokenListUploadResponse uploadFile(MultipartFile file) throws IOException {
        return batchAcquirerService.uploadFile(file);
    }

}
