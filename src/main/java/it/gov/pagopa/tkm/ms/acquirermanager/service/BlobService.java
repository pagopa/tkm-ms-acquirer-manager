package it.gov.pagopa.tkm.ms.acquirermanager.service;

import com.azure.storage.blob.models.*;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;

import java.time.Instant;
import java.util.List;

public interface BlobService {

    String uploadFile(byte[] fileByte, Instant instant, String filename, String sha256, BatchEnum batch);

    void downloadFileHashingTmp(String remotePathFile, String localPathFileOut);

    List<BlobItem> getFilesFromDirectory(String directory);

    void deleteTodayFolder(Instant now, BatchEnum batch);

}
