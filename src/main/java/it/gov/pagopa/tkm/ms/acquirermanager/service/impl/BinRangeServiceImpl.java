package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.VisaClient;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.CircuitEnum;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.BatchResultDetails;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBatchResult;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBinRange;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.BatchResultRepository;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.BinRangeRepository;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BinRangeService;
import it.gov.pagopa.tkm.ms.acquirermanager.thread.GenBinRangeCallable;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum.BIN_RANGE_GEN;
import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum.BIN_RANGE_RETRIEVAL;

@Service
@Log4j2
public class BinRangeServiceImpl implements BinRangeService {

    @Autowired
    private BinRangeRepository binRangeRepository;

    @Autowired(required = false)
    private VisaClient visaClient;

    @Autowired
    private BatchResultRepository batchResultRepository;

    @Autowired
    private ObjectMapper mapper;

    @Value("${batch.bin-range-gen.max-rows-in-files}")
    private int maxRowsInFiles;

    @Autowired
    private GenBinRangeCallable genBinRangeCallable;

    @Autowired
    private Tracer tracer;

    @Override
    public void generateBinRangeFiles() {
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
        batchResult.setDetails(writeAsJson(batchResultDetails));
        batchResult.setRunOutcome(batchResultDetails.stream().allMatch(BatchResultDetails::isSuccess));
        batchResultRepository.save(batchResult);
        log.info("End of bin range generation batch " + traceId);
    }

    private List<BatchResultDetails> executeThreads(Instant now) {
        List<Future<BatchResultDetails>> genBinRangeCallables = new ArrayList<>();
        long count = binRangeRepository.count();
        if (count == 0) {
            genBinRangeCallables.add(genBinRangeCallable.call(now, 0, 0, count));
        } else {
            executeMoreThanZeroRows(now, genBinRangeCallables, count);
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

    private void executeMoreThanZeroRows(Instant now, List<Future<BatchResultDetails>> genBinRangeCallables, long count) {
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

    private String writeAsJson(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public void retrieveVisaBinRanges() {
        Span span = tracer.currentSpan();
        String traceId = span != null ? span.context().traceId() : "noTraceId";
        log.info("Start of Visa bin range retrieval batch " + traceId);
        Instant start = Instant.now();
        TkmBatchResult result = TkmBatchResult.builder()
                .targetBatch(BIN_RANGE_RETRIEVAL)
                .runDate(start)
                .runOutcome(true)
                .executionTraceId(String.valueOf(traceId))
                .build();
        List<TkmBinRange> binRanges = new ArrayList<>();
        BatchResultDetails details;
        try {
            binRangeRepository.deleteByCircuit(CircuitEnum.VISA);
            log.info("Deleted old Visa bin ranges");
            binRanges = visaClient.getBinRanges();
            int size = CollectionUtils.size(binRanges);
            log.info(size + " token bin ranges retrieved");
            details = BatchResultDetails.builder().numberOfRows(size).success(true).build();
        } catch (Exception e) {
            log.error(e);
            result.setRunOutcome(false);
            details = BatchResultDetails.builder().success(false).errorMessage(e.getMessage()).build();
        }
        result.setRunDurationMillis(Instant.now().toEpochMilli() - start.toEpochMilli());
        result.setDetails(writeAsJson(details));
        batchResultRepository.save(result);
        binRangeRepository.saveAll(binRanges);
        log.info("End of Visa bin range retrieval batch");
    }

}
