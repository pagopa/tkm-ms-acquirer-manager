package it.gov.pagopa.tkm.ms.acquirermanager.repository;

import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBinRange;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import javax.persistence.QueryHint;
import java.util.stream.Stream;

import static org.hibernate.annotations.QueryHints.READ_ONLY;
import static org.hibernate.jpa.QueryHints.HINT_CACHEABLE;
import static org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE;

public interface BinRangeRepository extends JpaRepository<TkmBinRange, Long> {
    @QueryHints(value = {
            @QueryHint(name = HINT_FETCH_SIZE, value = "0"),
            @QueryHint(name = HINT_CACHEABLE, value = "false"),
            @QueryHint(name = READ_ONLY, value = "true")
    })
    @Query("select t from TkmBinRange t")
    Stream<TkmBinRange> getAll(Pageable pageRequest);
}
