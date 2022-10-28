package it.gov.pagopa.tkm.ms.acquirermanager.controller;

import it.gov.pagopa.tkm.ms.acquirermanager.model.response.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;

import java.io.*;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.ApiEndpoints.PUBLIC_KEY;
import static it.gov.pagopa.tkm.ms.acquirermanager.constant.ApiEndpoints.TOKEN_LIST;

@RequestMapping(TOKEN_LIST)
public interface TokenListController {

    @GetMapping(PUBLIC_KEY)
    String getPublicPgpKey();

    @PostMapping
    TokenListUploadResponse uploadFile(@RequestPart MultipartFile file) throws IOException;

}
