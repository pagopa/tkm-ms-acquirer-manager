package it.gov.pagopa.tkm.ms.acquirermanager.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class TestZipUtils {

    @TempDir
    static Path tempDir;

    static Path tempFile;

    @BeforeAll
    void init() throws IOException {
        tempFile = Files.createFile(tempDir.resolve("test2.txt"));
    }

    @Test
    void givenPath_returnZippedBytes() throws IOException {
        byte[] zippedFile = ZipUtils.zipFile(tempFile.toAbsolutePath().toString());
        assertEquals(134, zippedFile.length);
    }


}
