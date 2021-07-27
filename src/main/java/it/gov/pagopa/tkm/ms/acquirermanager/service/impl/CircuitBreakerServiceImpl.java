package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import io.github.resilience4j.retry.annotation.Retry;
import it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.model.request.VisaBinRangeRequest;
import it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.model.response.VisaBinRangeResponse;
import it.gov.pagopa.tkm.ms.acquirermanager.service.CircuitBreakerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class CircuitBreakerServiceImpl implements CircuitBreakerService {

    @Override
  //@CircuitBreaker(name = "visaBinRangesCircuitBreaker", fallbackMethod = "visaBinRangesFallback")
    @Retry(name = "visaBinRangesRetry", fallbackMethod = "visaBinRangesFallback")
    public VisaBinRangeResponse getVisaBinRanges(String retrieveBinRangesUrl, HttpEntity<VisaBinRangeRequest> entity,
                                                 RestTemplate restTemplate){
         return restTemplate.postForObject(retrieveBinRangesUrl, entity, VisaBinRangeResponse.class);

    }

    public VisaBinRangeResponse visaBinRangesFallback(String retrieveBinRangesUrl, HttpEntity<VisaBinRangeRequest> entity,
                                                      RestTemplate restTemplate,  Throwable t){
        log.info("VISA BIN RANGES fallback - cause {}", t.toString());
        return null;
    }

}
