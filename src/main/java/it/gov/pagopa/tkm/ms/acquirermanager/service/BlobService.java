package it.gov.pagopa.tkm.ms.acquirermanager.service;

import com.azure.storage.blob.models.BlobItem;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum;

import java.time.Instant;
import java.util.List;

public interface BlobService {

    String uploadAcquirerFile(byte[] fileByte, Instant instant, String filename, String sha256, BatchEnum batchEnum);

    void downloadFileHashingTmp(String remotePathFile, String localPathFileOut);

    List<BlobItem> getBlobItemsInFolderHashingTmp(String folderPath);

    void deleteTodayFolder(Instant now, BatchEnum batchEnum);
}
