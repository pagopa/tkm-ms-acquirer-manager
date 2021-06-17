package it.gov.pagopa.tkm.ms.acquirermanager.util;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Log4j2
public class ZipUtils {

    public static byte[] zipFile(String filePath) throws IOException {
        File file = new File(filePath);
        ZipEntry entry = new ZipEntry(file.getName());
        entry.setSize(FileUtils.sizeOf(file));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        zos.putNextEntry(entry);
        int length;
        byte[] buffer = new byte[1024];
        try (FileInputStream in = new FileInputStream(filePath)) {
            while ((length = in.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
        }
        zos.closeEntry();
        zos.close();
        log.debug("Zipped: " + filePath);
        Files.delete(Paths.get(filePath));
        log.debug("Deleted: " + filePath + " - Exists? " + Files.exists(Paths.get(filePath)));
        return baos.toByteArray();
    }

}
