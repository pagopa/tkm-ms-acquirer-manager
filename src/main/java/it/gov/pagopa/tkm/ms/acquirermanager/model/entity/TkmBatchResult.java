package it.gov.pagopa.tkm.ms.acquirermanager.model.entity;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import lombok.*;
import lombok.Builder;

import javax.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "BATCH_RESULT")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TkmBatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", unique = true, nullable = false)
    private Long id;

    @Column(name = "EXECUTION_UUID", unique = true, nullable = false)
    private UUID executionUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "TARGET_BATCH", nullable = false)
    private BatchEnum targetBatch;

    @Column(name = "RUN_DATE", nullable = false)
    private Instant runDate;

    @Column(name = "RUN_OUTCOME", nullable = false)
    private boolean runOutcome;

    @Column(name = "RUN_DURATION_MILLIS", nullable = false)
    private long runDurationMillis;

    @Column(name = "DETAILS", nullable = false)
    private String details;

}
