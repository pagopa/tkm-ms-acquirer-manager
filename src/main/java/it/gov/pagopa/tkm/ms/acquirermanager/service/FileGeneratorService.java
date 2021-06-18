package it.gov.pagopa.tkm.ms.acquirermanager.service;

import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.*;

import java.io.*;
import java.time.Instant;

public interface FileGeneratorService {

    BatchResultDetails generateFileWithStream(Instant now, int size, int index, long total, String fileName) throws IOException;
    BatchResultDetails generateHpanHtokenFileWithStream(Instant now, int maxItemPerPage, int pageNumber,
                                                        long total, String fileName, int fromPage, int toPage) throws IOException;

}
