package it.gov.pagopa.tkm.ms.acquirermanager.service;

import it.gov.pagopa.tkm.ms.acquirermanager.model.response.*;
import org.springframework.web.multipart.*;

import java.io.*;

public interface BatchAcquirerService {

    void queueBatchAcquirerResult();

    String getPublicPgpKey();

    TokenListUploadResponse uploadFile(MultipartFile file) throws IOException;

}
