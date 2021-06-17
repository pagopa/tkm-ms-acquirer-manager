package it.gov.pagopa.tkm.ms.acquirermanager.util;

import org.apache.commons.io.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.junit.jupiter.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
public class TestZipUtils {

    private final String tempFilePath = FileUtils.getTempDirectoryPath() + File.separator + "test.csv";

    @AfterAll
    void cleanup() throws IOException {
        Files.delete(Paths.get(tempFilePath));
    }

    @Test
    void givenPath_returnZippedBytes() throws IOException, DataFormatException {
        File file = new File(tempFilePath);
        FileUtils.write(file, "test");
        byte[] zippedFile = ZipUtils.zipFile(tempFilePath);
        assertEquals(, "test".getBytes());
    }

    public static byte[] extractZipEntries(byte[] content) throws IOException {
        ByteArrayInputStream binput = new ByteArrayInputStream(content);
        ZipInputStream zis = new ZipInputStream(binput);
        ZipEntry entry;
        while (null != (entry = zis.getNextEntry())) {
            FileOutputStream fos = new FileOutputStream(entry.getName());
            int index;
            byte[] buffer = new byte[1024];
            while (0 < (index = zis.read(buffer))) {
                fos.write(buffer, 0, index);
            }
            fos.close();
            zis.closeEntry();
        }
    }

}
