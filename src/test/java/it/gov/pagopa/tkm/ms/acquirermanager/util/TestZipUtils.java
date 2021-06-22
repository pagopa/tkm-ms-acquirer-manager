package it.gov.pagopa.tkm.ms.acquirermanager.util;

import com.google.common.io.ByteStreams;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class TestZipUtils {

    @TempDir
    static Path tempDir;

    private Random random = new Random();

    @Test
    void givenPath_returnZippedBytes() throws IOException {
        Path tempFile = Files.createFile(tempDir.resolve("toZip.txt"));
        byte[] zippedFile = ZipUtils.zipFile(tempFile.toAbsolutePath().toString());
        assertEquals(134, zippedFile.length);
    }

    @SuppressWarnings("UnstableApiUsage")
    @Test
    void unzipFile_success() throws IOException {
        String tempFileZip = Files.createFile(tempDir.resolve(random.nextInt() + ".zip")).toAbsolutePath().toString();
        byte[] bytes = ByteStreams.toByteArray(new ClassPathResource("junit.zip").getInputStream());
        FileUtils.writeByteArrayToFile(new File(tempFileZip), bytes);
        List<String> stringList = ZipUtils.unzipFile(tempFileZip, tempDir.toAbsolutePath().toString());
        assertEquals(1, stringList.size());
        String messageDecrypted = Files.lines(Paths.get(stringList.get(0)), StandardCharsets.UTF_8).collect(Collectors.joining(System.lineSeparator()));
        assertEquals("message", messageDecrypted);
    }

}
