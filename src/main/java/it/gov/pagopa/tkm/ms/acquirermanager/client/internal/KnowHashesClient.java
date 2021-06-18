package it.gov.pagopa.tkm.ms.acquirermanager.client.internal;

import feign.Response;
import org.hibernate.validator.constraints.Range;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;

@FeignClient(value = "know-hashes", url = "${client-urls.card-manager}")
public interface KnowHashesClient {


    String MAX_NUMBER_OF_RECORD_DEFAULT = "100000";
    String PAGE_NUMBER_DEFAULT = "0";
    static final String PAGE_NUMBER_PARAM = "pageNumber";
    static final String MAX_NUMBER_OF_RECORDS_PARAM = "maxNumberOfRecord";
    int MIN_VALUE = 10;
    int MAX_VALUE = 1000000;


    @GetMapping("/known-hash/pan")
    Response getKnownHashPanSet(
            @Valid
            @RequestParam(value = MAX_NUMBER_OF_RECORDS_PARAM, defaultValue = MAX_NUMBER_OF_RECORD_DEFAULT)
            @Range(min = MIN_VALUE, max = MAX_VALUE)
                    Integer maxRecords,
            @RequestParam(value = PAGE_NUMBER_PARAM, defaultValue = PAGE_NUMBER_DEFAULT)
                    Integer pageNumber
    );

    @GetMapping("/known-hash/token")
    Response getKnownHashTokenSet(
            @Valid
            @RequestParam(value = MAX_NUMBER_OF_RECORDS_PARAM, defaultValue = MAX_NUMBER_OF_RECORD_DEFAULT)
            @Range(min = MIN_VALUE, max = MAX_VALUE)
                    Integer maxRecords,
            @RequestParam(value = PAGE_NUMBER_PARAM, defaultValue = PAGE_NUMBER_DEFAULT)
                    Integer pageNumber
    );

}
