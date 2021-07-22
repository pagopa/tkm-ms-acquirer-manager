package it.gov.pagopa.tkm.ms.acquirermanager.service;

import it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.model.request.VisaBinRangeRequest;
import it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.model.response.VisaBinRangeResponse;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

public interface CircuitBreakerService {

    VisaBinRangeResponse getVisaBinRanges(String retrieveBinRangesUrl, HttpEntity<VisaBinRangeRequest> entity, RestTemplate restTemplate);

}
