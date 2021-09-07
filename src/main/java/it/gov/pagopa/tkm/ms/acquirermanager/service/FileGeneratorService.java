package it.gov.pagopa.tkm.ms.acquirermanager.service;

import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.*;
import org.springframework.transaction.annotation.*;

import java.io.*;
import java.time.Instant;
import java.util.*;

public interface FileGeneratorService {

    @Transactional(readOnly = true)
    BatchResultDetails generateBinRangesFile(Instant now, int size, int index, long total) throws IOException;

    @Transactional(readOnly = true)
    BatchResultDetails generateKnownHashesFile(Instant now, int index, List<String> hashes);

}
