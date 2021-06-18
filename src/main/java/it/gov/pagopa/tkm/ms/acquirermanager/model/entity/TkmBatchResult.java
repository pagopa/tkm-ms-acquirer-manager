package it.gov.pagopa.tkm.ms.acquirermanager.model.entity;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.time.*;

@Entity
@Table(name = "BATCH_RESULT")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Log4j2
public class TkmBatchResult {

    public static final int DETAILS_LEN = 10000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", unique = true, nullable = false)
    private Long id;

    @Column(name = "EXECUTION_TRACE_ID", nullable = false)
    private String executionTraceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "TARGET_BATCH", nullable = false)
    private BatchEnum targetBatch;

    @Column(name = "RUN_DATE", nullable = false)
    private Instant runDate;

    @Column(name = "RUN_OUTCOME", nullable = false)
    private boolean runOutcome;

    @Column(name = "RUN_DURATION_MILLIS", nullable = false)
    private long runDurationMillis;

    @Column(name = "DETAILS")
    private String details;

    @PreUpdate
    @PrePersist
    public void truncateDetails() {
        if (StringUtils.isNotBlank(details) && details.length() > DETAILS_LEN) {
            log.warn("Truncation TkmBatchResult.details because is too long. Char:" + details.length());
            details = StringUtils.truncate(details, DETAILS_LEN - 3) + "...";
        }
    }
}
