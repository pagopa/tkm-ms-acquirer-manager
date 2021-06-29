package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.tkm.ms.acquirermanager.client.internal.cardmanager.CardManagerClient;
import it.gov.pagopa.tkm.ms.acquirermanager.client.internal.cardmanager.model.response.KnownHashesResponse;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.DirectoryNames;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.ErrorCodeEnum;
import it.gov.pagopa.tkm.ms.acquirermanager.exception.AcquirerDataNotFoundException;
import it.gov.pagopa.tkm.ms.acquirermanager.exception.AcquirerException;
import it.gov.pagopa.tkm.ms.acquirermanager.exception.EmptyResponseException;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.BatchResultDetails;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBatchResult;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmHashOffset;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.BatchResultRepository;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.HashOffsetRepository;
import it.gov.pagopa.tkm.ms.acquirermanager.service.FileGeneratorService;
import it.gov.pagopa.tkm.ms.acquirermanager.service.KnownHashesGenService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum.KNOWN_HASHES_GEN;

@Service
@Log4j2
public class KnownHashesGenServiceImpl implements KnownHashesGenService {

    @Autowired
    private BatchResultRepository batchResultRepository;

    @Autowired
    private HashOffsetRepository hashOffsetRepository;

    @Autowired
    private ObjectMapper mapper;

    @Value("${batch.known-hashes-gen.max-rows-in-files}")
    private int maxRowsInFiles;

    @Value("${batch.known-hashes-gen.max-records-in-api-call}")
    private int maxRecordsInApiCall;

    @Autowired
    private FileGeneratorService fileGeneratorService;

    @Autowired
    private Tracer tracer;

    @Autowired
    private CardManagerClient cardManagerClient;

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${BLOB_STORAGE_BIN_HASH_CONTAINER}")
    private String containerName;

    private final BlobServiceClientBuilder serviceClientBuilder = new BlobServiceClientBuilder();

    @Override
    public void generateKnownHashesFiles() {
        Span span = tracer.currentSpan();
        String traceId = span != null ? span.context().traceId() : "noTraceId";
        log.info("Start of known hashes generation batch " + traceId);
        Instant now = Instant.now();
        long start = now.toEpochMilli();
        List<BatchResultDetails> batchResultDetails = new ArrayList<>();
        TkmBatchResult batchResult = TkmBatchResult.builder()
                .targetBatch(KNOWN_HASHES_GEN)
                .executionTraceId(String.valueOf(traceId))
                .runDate(now)
                .build();
        try {
            batchResultDetails = getKnownHpans(now);
        } catch (Exception e) {
            log.error(e);
            batchResultDetails.add(BatchResultDetails.builder().success(false).errorMessage(e.getMessage()).build());
        }
        long duration = Instant.now().toEpochMilli() - start;
        batchResult.setRunDurationMillis(duration);
        batchResult.setDetails(writeAsJson(batchResultDetails));
        batchResult.setRunOutcome(batchResultDetails.stream().allMatch(BatchResultDetails::isSuccess));
        batchResultRepository.save(batchResult);
        log.info("End of known hashes generation batch " + traceId);
    }

    private String writeAsJson(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private List<BatchResultDetails> getKnownHpans(Instant now) throws AcquirerException {
        List<BatchResultDetails> details = new ArrayList<>();
        List<TkmHashOffset> offsets = hashOffsetRepository.findAll();
        TkmHashOffset lastOffset = CollectionUtils.isEmpty(offsets) ? new TkmHashOffset() : offsets.get(0);
        List<String> hashes = callCardManagerForHashes(lastOffset);
        int freeSpotsInLastFile = lastOffset.getFreeSpots(maxRowsInFiles);
        if (CollectionUtils.isNotEmpty(hashes) && freeSpotsInLastFile > 0) {
            BatchResultDetails lastFileDetails = manageExistingFile(lastOffset, hashes, freeSpotsInLastFile);
            details.add(lastFileDetails);
        }
        List<String> remainingHashes = hashes.stream().skip(freeSpotsInLastFile).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hashes) && lastOffset.getLastHashesFileIndex() == 0) {
            remainingHashes = Collections.singletonList(StringUtils.EMPTY);
        }
        if (CollectionUtils.isNotEmpty(remainingHashes)) {
            List<List<String>> partitionedHashes = ListUtils.partition(remainingHashes, maxRowsInFiles);
            for (List<String> chunk : partitionedHashes) {
                lastOffset.increaseIndex();
                BatchResultDetails newFileDetails = manageNewFile(chunk, now, lastOffset.getLastHashesFileIndex(), lastOffset);
                details.add(newFileDetails);
            }
        }
        log.trace(lastOffset);
        hashOffsetRepository.save(lastOffset);
        return details;
    }

    private List<String> callCardManagerForHashes(TkmHashOffset lastOffset) throws AcquirerException {
        try {
            log.info("Calling Card Manager for known hashes");
            KnownHashesResponse hashesResponse = cardManagerClient.getKnownHashes(maxRecordsInApiCall, lastOffset.getLastHpanOffset(), lastOffset.getLastHtokenOffset());
            checkResponse(hashesResponse);
            List<String> hashes = ListUtils.union(hashesResponse.getHpans(), hashesResponse.getHtokens());
            log.info(hashes.size() + " hashes retrieved");
            lastOffset.setLastHpanOffset(hashesResponse.getNextHpanOffset());
            lastOffset.setLastHtokenOffset(hashesResponse.getNextHtokenOffset());
            return hashes;
        } catch (Exception e) {
            throw new AcquirerException(ErrorCodeEnum.CALL_TO_CARD_MANAGER_FAILED);
        }
    }

    private void checkResponse(Object response) throws EmptyResponseException {
        if (response == null) {
            String responseCannotBeEmpty = "Response cannot be empty";
            log.error(responseCannotBeEmpty);
            throw new EmptyResponseException(responseCannotBeEmpty);
        }
    }

    private BatchResultDetails manageNewFile(List<String> remainingHashes, Instant now, int index, TkmHashOffset lastOffset) {
        log.info("Writing " + remainingHashes.size() + " hashes to new file");
        log.trace(remainingHashes);
        BatchResultDetails newFileDetails = new BatchResultDetails();
        try {
            newFileDetails = fileGeneratorService.generateKnownHashesFile(now, index, remainingHashes);
        } catch (Exception e) {
            log.error(e);
            newFileDetails.setErrorMessage(e.getMessage());
            newFileDetails.setSuccess(false);
        }
        lastOffset.setLastHashesFileIndex(index);
        lastOffset.setLastHashesFileFilename(newFileDetails.getFileName());
        lastOffset.setLastHashesFileRowCount(ObjectUtils.firstNonNull(newFileDetails.getNumberOfRows(), 0));
        return newFileDetails;
    }

    private BatchResultDetails manageExistingFile(TkmHashOffset lastOffset, List<String> hashes, int freeSpotsInLastFile) {
        log.info("Last generated hashes file has " + freeSpotsInLastFile + " free spots, filling them up");
        String filename = lastOffset.getLastHashesFileFilename();
        BatchResultDetails lastFileDetails = BatchResultDetails.builder().success(true).fileName(filename).build();
        try {
            BlobItem lastFile = getLastGeneratedFile(filename);
            log.info("Found " + lastFile.getName());
            List<String> hashesSublist = hashes.stream().limit(freeSpotsInLastFile).collect(Collectors.toList());
            updateFile(lastFile, hashesSublist);
            int newRowCount = lastOffset.getLastHashesFileRowCount() + hashesSublist.size();
            lastOffset.setLastHashesFileFilename(filename);
            lastOffset.setLastHashesFileRowCount(newRowCount);
            lastFileDetails.setNumberOfRows(newRowCount);
        } catch (Exception e) {
            log.error(e);
            lastFileDetails.setErrorMessage(e.getMessage());
            lastFileDetails.setSuccess(false);
        }
        return lastFileDetails;
    }

    private void updateFile(BlobItem file, List<String> hashes) {
        log.info("Updating file " + file.getName());
        String hashesAsString = hashes.stream().collect(Collectors.joining(System.lineSeparator())) + System.lineSeparator();
        byte[] hashesAsBytes = hashesAsString.getBytes();
        log.trace(hashesAsBytes);
        BlobServiceClient serviceClient = serviceClientBuilder.connectionString(connectionString).buildClient();
        BlobContainerClient client = serviceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = client.getBlobClient(file.getName());
        blobClient.getAppendBlobClient().appendBlock(new ByteArrayInputStream(hashesAsBytes), hashesAsBytes.length);
        log.info("File updated");
    }

    private BlobItem getLastGeneratedFile(String filename) {
        BlobServiceClient serviceClient = serviceClientBuilder.connectionString(connectionString).buildClient();
        BlobContainerClient client = serviceClient.getBlobContainerClient(containerName);
        String completePath = String.format("%s/%s/%s", DirectoryNames.KNOWN_HASHES, DirectoryNames.ALL_KNOWN_HASHES, filename);
        log.info("Looking for file: " + completePath);
        BlobListDetails blobListDetails = new BlobListDetails().setRetrieveMetadata(true);
        ListBlobsOptions listBlobsOptions = new ListBlobsOptions()
                .setPrefix(completePath)
                .setDetails(blobListDetails);
        List<BlobItem> blobItemList = new ArrayList<>();
        client.listBlobsByHierarchy("/", listBlobsOptions, null).iterator().forEachRemaining(blobItemList::add);
        if (CollectionUtils.isEmpty(blobItemList)) {
            log.warn("No files found for given filename");
            throw new AcquirerDataNotFoundException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        return blobItemList.get(0);
    }

}
