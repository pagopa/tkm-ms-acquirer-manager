package it.gov.pagopa.tkm.ms.acquirermanager.model.entity;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import lombok.*;
import lombok.Builder;

import javax.persistence.*;
import java.time.*;

@Entity
@Table(name = "BIN_RANGE")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TkmBinRange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", unique = true, nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "CIRCUIT", nullable = false)
    private CircuitEnum circuit;

    @Column(name = "MIN", nullable = false)
    private String min;

    @Column(name = "MAX", nullable = false)
    private String max;

    @Column(name = "INSERT_DATE", nullable = false)
    private Instant insertDate;

}
