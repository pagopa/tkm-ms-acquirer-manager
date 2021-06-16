package it.gov.pagopa.tkm.ms.acquirermanager.model.entity;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import lombok.*;
import lombok.experimental.*;

import javax.persistence.*;
import java.time.*;

@Entity
@Table(name = "BATCH_RESULT")
@Data
@Accessors(chain = true)
public class TkmBatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", unique = true, nullable = false)
    private Long id;

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
