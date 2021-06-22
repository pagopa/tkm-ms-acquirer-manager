package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import it.gov.pagopa.tkm.ms.acquirermanager.service.BatchAcquirerService;
import it.gov.pagopa.tkm.ms.acquirermanager.util.PgpUtils;
import it.gov.pagopa.tkm.ms.acquirermanager.util.ZipUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@Log4j2
public class BatchAcquirerServiceImpl implements BatchAcquirerService {

    @Value("${keyvault.acquirerPgpPrivateKeyPassphrase}")
    private char[] pgpPassPhrase;

    @Value("${keyvault.acquirerPgpPrivateKey}")
    private byte[] pgpPrivateKey;

    @Override
    public void queueBatchAcquirerResult() {

        log.info("Read and unzip file");
        String zipFilePath = "C:\\Users\\a09u\\OneDrive - GFT Technologies SE\\Desktop\\ToDel\\acquirer.zip";
        workOnFile(zipFilePath);
    }

    private void workOnFile(String zipFilePath) {
        String destDirectory = FileUtils.getTempDirectoryPath() + File.separator + UUID.randomUUID();
        try {
            List<String> files = getUnzippedFile(zipFilePath, destDirectory);
            log.debug("Unzipped file to " + files);
            String fileInputPgp = files.get(0);
            String fileOutputClear = fileInputPgp + ".clear";
            log.debug("File decrypted " + fileOutputClear);
            PgpUtils.decrypt(fileInputPgp, pgpPrivateKey, pgpPassPhrase, fileOutputClear);
        } catch (Exception e) {
            log.error("Failed to elaborate: " + zipFilePath, e);
        } finally {
            deleteDirectoryQuietly(destDirectory);
        }
    }

    private void deleteDirectoryQuietly(String destDirectory) {
        log.debug("Deleting " + destDirectory);
        try {
            FileUtils.deleteDirectory(new File(destDirectory));
        } catch (IOException e) {
            log.error("Cannot delete directory");
        }
    }

    private List<String> getUnzippedFile(String zipFilePath, String destDirectory) throws IOException {
        List<String> files = ZipUtils.unzipFile(zipFilePath, destDirectory);
        if (IterableUtils.size(files) != 1) {
            throw new IOException("Too Many unzipped files");
        }
        return files;
    }

    private void parseCSVFile(String filePath) {

    }

}
