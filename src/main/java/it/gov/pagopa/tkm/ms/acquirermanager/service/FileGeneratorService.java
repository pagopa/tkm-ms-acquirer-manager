package it.gov.pagopa.tkm.ms.acquirermanager.service;

import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.*;

import java.io.*;
import java.time.Instant;

public interface FileGeneratorService {

    BatchResultDetails generateFileWithStream(Instant now, int size, int index, long total, String fileName) throws IOException;

}
