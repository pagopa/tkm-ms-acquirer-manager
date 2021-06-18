package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import com.azure.storage.blob.*;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import it.gov.pagopa.tkm.constant.TkmDatetimeConstant;
import it.gov.pagopa.tkm.ms.acquirermanager.client.internal.KnowHashesClient;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum;
import it.gov.pagopa.tkm.ms.acquirermanager.exception.AcquirerDataNotFoundException;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.BatchResultDetails;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBatchResult;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.LinksResponse;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.BatchResultRepository;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.BinRangeRepository;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BinRangeHashService;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BlobService;
import it.gov.pagopa.tkm.ms.acquirermanager.service.FileGeneratorService;
import it.gov.pagopa.tkm.ms.acquirermanager.thread.GenBinRangeCallable;
import it.gov.pagopa.tkm.ms.acquirermanager.thread.GenHpanHtokenCallable;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum.BIN_RANGE_GEN;
import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum.HTOKEN_HPAN_GEN;
import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BlobMetadataEnum.generationdate;

@Service
@Log4j2
public class BinRangeHashServiceImpl implements BinRangeHashService {

    @Autowired
    private BinRangeRepository binRangeRepository;

    @Autowired
    private BatchResultRepository batchResultRepository;

    @Autowired
    private ObjectMapper mapper;

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${BLOB_STORAGE_BIN_HASH_CONTAINER}")
    private String containerName;

    @Value("${batch.bin-range-gen.max-rows-in-files}")
    private int maxRowsInFiles;

    @Value("${AZURE_KEYVAULT_PROFILE}")
    private String profile;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private FileGeneratorService fileGeneratorService;

    @Autowired
    private BlobService blobService;

    @Autowired
    private GenBinRangeCallable genBinRangeCallable;

    @Autowired
    private GenHpanHtokenCallable genHpanHtpkenCallable;

    @Autowired
    private KnowHashesClient knowHashesClient;

    @Autowired
    private Tracer tracer;

    private final BlobServiceClientBuilder serviceClientBuilder = new BlobServiceClientBuilder();
    private final BlobClientBuilder blobClientBuilder = new BlobClientBuilder();
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("uuuuMMdd").withZone(ZoneId.of(TkmDatetimeConstant.DATE_TIME_TIMEZONE));
    private final int MAX_NUMBER_OF_HPAN_HTOKEN = 1000000;
    private final int REQUESTED_NUMBER_OF_HPAN_HTOKEN_PER_FILE = 100000;

    @Override
    public LinksResponse getSasLinkResponse(BatchEnum batchEnum) {
        log.info("Getting files for batch: " + batchEnum);
        BlobServiceClient serviceClient = serviceClientBuilder.connectionString(connectionString).buildClient();
        BlobContainerClient client = serviceClient.getBlobContainerClient(containerName);
        List<BlobItem> blobItems = getBlobItems(client, batchEnum);
        int blobItemsSize = CollectionUtils.size(blobItems);
        log.info(blobItemsSize + " files found on Blob Storage");
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime offsetDateTime = now.plusMinutes(getAvailableFor(blobItemsSize));
        List<String> links = getLinks(offsetDateTime, client, blobItems);
        LinksResponse response = LinksResponse.builder()
                .fileLinks(links)
                .numberOfFiles(links.size())
                .availableUntil(offsetDateTime.toInstant())
                .generationDate(getGenerationDate(blobItems))
                .expiredIn(offsetDateTime.toEpochSecond() - now.toEpochSecond())
                .build();
        log.debug("Response: " + response.toString());
        return response;
    }

    private Instant getGenerationDate(List<BlobItem> blobItems) {
        Instant instant = null;
        Map<String, String> genDate = blobItems.stream().findFirst().map(BlobItem::getMetadata).orElse(null);
        if (genDate != null) {
            String generationDate = genDate.get(generationdate.name());
            instant = StringUtils.isNotBlank(generationDate) ? Instant.parse(generationDate) : null;
        }
        return instant;
    }

    private List<BlobItem> getBlobItems(BlobContainerClient client, BatchEnum batchEnum) {
        Instant now = Instant.now();
        String directory = String.format("%s/%s/", batchEnum, dateFormat.format(now));
        log.info("Looking for directory: " + directory);
        BlobListDetails blobListDetails = new BlobListDetails().setRetrieveMetadata(true);
        ListBlobsOptions listBlobsOptions = new ListBlobsOptions()
                .setPrefix(directory)
                .setDetails(blobListDetails);
        List<BlobItem> blobItemList = new ArrayList<>();
        client.listBlobsByHierarchy("/", listBlobsOptions, null).iterator().forEachRemaining(blobItemList::add);
        if (CollectionUtils.isEmpty(blobItemList)) {
            log.info("No files found on Blob Storage");
            throw new AcquirerDataNotFoundException();
        }
        return blobItemList;
    }

    private List<String> getLinks(OffsetDateTime expireTime, BlobContainerClient client, List<BlobItem> blobItems) {
        BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(expireTime, new BlobContainerSasPermission().setReadPermission(true));
        List<String> links = new ArrayList<>();
        String completeContainerUrl = client.getBlobContainerUrl();
        for (BlobItem blobItem : blobItems) {
            String blobName = blobItem.getName();
            log.trace(blobName);
            BlobClient blobClient = blobClientBuilder
                    .connectionString(connectionString)
                    .blobName(blobName)
                    .containerName(containerName)
                    .buildClient();
            links.add(String.format("%s/%s?%s", completeContainerUrl, blobName, blobClient.generateSas(sasValues)));
        }
        return links;
    }

    private long getAvailableFor(long linksSize) {
        return NumberUtils.min(10, linksSize * 2);
    }

    private List<BatchResultDetails> executeThreads(Instant now) {
        List<Future<BatchResultDetails>> genBinRangeCallables = new ArrayList<>();
        long count = binRangeRepository.count();
        if (count == 0) {
            genBinRangeCallables.add(genBinRangeCallable.call(now, 0, 0, count));
        } else {
            executeMoreThanZeroRow(now, genBinRangeCallables, count);
        }
        return genBinRangeCallables.stream().map(t -> {
            try {
                return t.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("detailsFutures", e);
                Thread.currentThread().interrupt();
                return BatchResultDetails.builder().success(false).errorMessage(e.getMessage()).build();
            }
        }).collect(Collectors.toList());
    }

    private void executeMoreThanZeroRow(Instant now, List<Future<BatchResultDetails>> genBinRangeCallables, long count) {
        int ceil;
        int rowInFile = maxRowsInFiles;
        ceil = (int) Math.ceil(count / (double) maxRowsInFiles);
        if (ceil > 10) {
            ceil = 10;
            rowInFile = (int) Math.ceil(count / (double) ceil);
        }
        for (int i = 0; i < ceil; i++) {
            genBinRangeCallables.add(genBinRangeCallable.call(now, rowInFile, i, count));
        }
    }

    private void executeMoreThanZeroHpanHtokenRow(Instant now, List<Future<BatchResultDetails>> genHpanHtokenCallables,
                                                  long numberOfPages, int maxNumberInPage, int numberOfPagePerThread, int numberOfThreads) {
        int fromPage=0;
        int toPage=0;
        for (int i = 0; i < numberOfThreads; i++) {
            toPage+=numberOfPagePerThread;
            genHpanHtokenCallables.add(genHpanHtpkenCallable.call(now, maxNumberInPage, i, numberOfPages, fromPage, toPage));
            fromPage+=toPage;
        }
    }


    private List<BatchResultDetails> executeThreadsHpanHtoken(Instant now) {

       List<Future<BatchResultDetails>> genHpanHtokenCallables = new ArrayList<>();
        int numberOfRecordPerFile= REQUESTED_NUMBER_OF_HPAN_HTOKEN_PER_FILE;
        double numberOfPagePerThread =  1;

        Response response = knowHashesClient.getKnownHashTokenSet(MAX_NUMBER_OF_HPAN_HTOKEN, 0);
        List<String> totalNumberPagesHeader = (List<String>) response.headers().get("Total-Number-Pages");
        String totalNumberPagesHeaderValue = totalNumberPagesHeader.get(0);
        long totalNumberPages =  Long.parseLong(totalNumberPagesHeaderValue);
        int numberOfThreads = (int)totalNumberPages;
        if (numberOfRecordPerFile>MAX_NUMBER_OF_HPAN_HTOKEN){
            numberOfRecordPerFile = MAX_NUMBER_OF_HPAN_HTOKEN;
            numberOfPagePerThread= Math.ceil(REQUESTED_NUMBER_OF_HPAN_HTOKEN_PER_FILE/numberOfRecordPerFile);
            numberOfThreads = (int)Math.floor(REQUESTED_NUMBER_OF_HPAN_HTOKEN_PER_FILE/numberOfRecordPerFile);
        }

        //associare ogni thread ad ogni file, quindi prenderà N pagine fino a che ho il numero di record richiesti per file
        //es 3 milioni di record, chiedo 2 milioni per file quindi ho un thread con due pagine che genera un file da 2 milioni
        // e un thread con un milione rimanente

        long itemPerPage = totalNumberPages;

          if (totalNumberPages == 0) {
            genHpanHtokenCallables.add(genBinRangeCallable.call(now, 0, 0, totalNumberPages));
        } else {
            executeMoreThanZeroHpanHtokenRow(now, genHpanHtokenCallables, totalNumberPages, MAX_NUMBER_OF_HPAN_HTOKEN, (int)numberOfPagePerThread, numberOfThreads );
        }
        return genHpanHtokenCallables.stream().map(t -> {
            try {
                return t.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("detailsFutures", e);
                Thread.currentThread().interrupt();
                return BatchResultDetails.builder().success(false).errorMessage(e.getMessage()).build();
            }
        }).collect(Collectors.toList());
    }


    @Override
    public void generateBinRangeFiles() throws JsonProcessingException {
        Span span = tracer.currentSpan();
        String traceId = span != null ? span.context().traceId() : "noTraceId";
        log.info("Start of bin range generation batch " + traceId);
        Instant now = Instant.now();
        long start = now.toEpochMilli();
        TkmBatchResult batchResult = TkmBatchResult.builder()
                .targetBatch(BIN_RANGE_GEN)
                .executionTraceId(String.valueOf(traceId))
                .runDate(now)
                .runOutcome(true)
                .build();
        List<BatchResultDetails> batchResultDetails = executeThreads(now);
        long duration = Instant.now().toEpochMilli() - start;
        batchResult.setRunDurationMillis(duration);
        batchResult.setDetails(mapper.writeValueAsString(batchResultDetails));
        batchResult.setRunOutcome(batchResultDetails.stream().allMatch(BatchResultDetails::isSuccess));
        batchResultRepository.save(batchResult);
        log.info("End of bin range generation batch");
    }

    @Override
    public void generateHpanHtokenFiles() throws JsonProcessingException {
        System.out.println("\n -------------  Start of hpan htoken generation batch 0");
        Span span = tracer.currentSpan();
        String traceId = span != null ? span.context().traceId() : "noTraceId";
        log.info("Start of hpan htoken generation batch " + traceId);
        Instant now = Instant.now();
        long start = now.toEpochMilli();
        System.out.println("\n -------------  Start of hpan htoken generation batch 1");

        TkmBatchResult batchResult = TkmBatchResult.builder()
                .targetBatch(HTOKEN_HPAN_GEN)
                .executionTraceId(String.valueOf(traceId))
                .runDate(now)
                .runOutcome(true)
                .build();
        System.out.println("\n -------------  Start of hpan htoken generation batch 2");

        List<BatchResultDetails> batchResultDetails = executeThreadsHpanHtoken(now);
        long duration = Instant.now().toEpochMilli() - start;
        batchResult.setRunDurationMillis(duration);
        batchResult.setDetails(mapper.writeValueAsString(batchResultDetails));
        batchResult.setRunOutcome(batchResultDetails.stream().allMatch(BatchResultDetails::isSuccess));
        batchResultRepository.save(batchResult);
        log.info("End of bin range generation batch");
    }

}
