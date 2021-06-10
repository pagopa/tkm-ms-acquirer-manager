package it.gov.pagopa.tkm.ms.acquirermanager.model.entity;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import lombok.*;
import lombok.experimental.*;

import javax.persistence.*;
import java.time.*;

@Entity
@Table(name = "FILE")
@Data
@Accessors(chain = true)
public class TkmFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", unique = true, nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "TARGET_BATCH", nullable = false)
    private BatchEnum targetBatch;

    @Column(name = "GEN_DATE", nullable = false)
    private Instant genDate;

    @Column(name = "GEN_OUTCOME", nullable = false)
    private boolean genOutcome;

    @Column(name = "DETAILS", nullable = false)
    private String details;

}
