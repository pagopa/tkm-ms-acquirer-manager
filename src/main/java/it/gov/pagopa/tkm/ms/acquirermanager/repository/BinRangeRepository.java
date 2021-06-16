package it.gov.pagopa.tkm.ms.acquirermanager.repository;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.*;
import org.springframework.data.jpa.repository.*;

public interface BinRangeRepository extends JpaRepository<TkmBinRange, Long> {

    void deleteByCircuit(CircuitEnum circuit);

}
