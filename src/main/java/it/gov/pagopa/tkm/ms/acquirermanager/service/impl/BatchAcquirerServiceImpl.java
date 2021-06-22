package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import com.opencsv.CSVWriter;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BatchAcquirerService;
import it.gov.pagopa.tkm.ms.acquirermanager.util.PgpUtils;
import it.gov.pagopa.tkm.ms.acquirermanager.util.ZipUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Paths;

@Service
@Log4j2
public class BatchAcquirerServiceImpl implements BatchAcquirerService {

    @Value("${keyvault.acquirerPgpPrivateKey}")
    private char[] pgpPassPhrase;

    @Value("${keyvault.acquirerPgpPrivateKeyPassphrase}")
    private byte[] pgpPrivateKey;

    @Override
    public void queueBatchAcquirerResult() throws Exception {

        log.info("Read and unzip file");
        String zipFilePath = "C:/Users/marco/Downloads/TKM.12345.TKNLST.20210621.105000.001.zip";
        String destDirectory = "C:/Users/marco/Desktop/Nuova cartella";
        ZipUtils.unzipFile(zipFilePath, destDirectory);
        File pgpFile = new File(destDirectory+"TKM.12345.TKNLST.20210621.105000.001.csv.pgp");
        byte[] pgpFileByteArray = FileUtils.readFileToByteArray(pgpFile);
        //decrypt pgp csv file
//        byte[] decryptedFileBytes = PgpUtils.decrypt(pgpFileByteArray, pgpPrivateKey, pgpPassPhrase);
//        writeCSVFileFromByteArray(decryptedFileBytes);
        //parse csv file
        String filePath = "";
        parseCSVFile(filePath);
        //send to queue

    }

    private void parseCSVFile(String filePath) {

    }

    private void writeCSVFileFromByteArray(byte[] decryptedFileBytes) throws IOException {

        // magari scriverlo in cartella/file temporaneo?
        String decoded = new String(decryptedFileBytes, "UTF-8");
        FileOutputStream fos = new FileOutputStream("C:/Users/marco/Desktop/Nuova cartella/decrypted.csv");
        OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
        CSVWriter writer = new CSVWriter(osw);
        String[] row = {decoded};

        writer.writeNext(row);
        writer.close();
        osw.close();
    }

}
