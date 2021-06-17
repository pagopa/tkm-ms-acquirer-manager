package it.gov.pagopa.tkm.ms.acquirermanager.service;

import java.time.Instant;

public interface BlobService {

    void uploadAcquirerFile(byte[] fileByte, Instant instant, String filename, String sha256);

}
