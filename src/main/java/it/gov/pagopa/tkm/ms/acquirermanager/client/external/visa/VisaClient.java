package it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.model.request.VisaBinRangeRequest;
import it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.model.request.VisaBinRangeRequestData;
import it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.model.request.VisaBinRangeRequestHeader;
import it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.model.response.VisaBinRangeResponse;
import it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.model.response.VisaBinRangeResponseData;
import it.gov.pagopa.tkm.ms.acquirermanager.exception.EmptyResponseException;
import it.gov.pagopa.tkm.ms.acquirermanager.exception.MissingPropertyException;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBinRange;
import it.gov.pagopa.tkm.ms.acquirermanager.service.CircuitBreakerService;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.CircuitBreakerServiceImpl;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Log4j2
@ConditionalOnExpression("'${batch.bin-range-retrieval.cron}' != '-'")
public class VisaClient {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private CircuitBreakerService circuitBreakerService;

    @Value("${blob-storage.visaPublicCert}")
    private Resource publicCert;

    @Value("${keyvault.visaKeyStorePassword}")
    private String keystorePassword;

    @Value("${keyvault.visaUserId}")
    private String userId;

    @Value("${keyvault.visaPassword}")
    private String password;

    @Value("${keyvault.visaKeyId}")
    private String keyId;

    @Value("${circuit-urls.visa}")
    private String retrieveBinRangesUrl;

    private static final Integer CHUNK_SIZE = 500;

    private static final int TIMEOUT = 5000;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
        log.info("Set Visa Client with Mutual Auth");
        log.debug(String.format("keystorePassword is blank: %s, userId is blank: %s, password is blank: %s, keyId is blank: %s, retrieveBinRangesUrl is blank: %s",
                StringUtils.isBlank(keystorePassword), StringUtils.isBlank(userId), StringUtils.isBlank(password),
                StringUtils.isBlank(keyId), StringUtils.isBlank(retrieveBinRangesUrl)));

        if (StringUtils.isAnyBlank(keystorePassword, userId, password, keyId, retrieveBinRangesUrl)) {
            throw new MissingPropertyException("Visa Client configuration has not been completed. Check the application.yml");
        }
        char[] chars = keystorePassword.toCharArray();
        KeyStore clientStore = KeyStore.getInstance("PKCS12");
        clientStore.load(publicCert.getInputStream(), chars);

        SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
        sslContextBuilder.loadKeyMaterial(clientStore, chars);

        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContextBuilder.build());
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(sslConnectionSocketFactory)
                .build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setConnectTimeout(TIMEOUT);
        requestFactory.setReadTimeout(TIMEOUT);

        restTemplate = new RestTemplate(requestFactory);
    }

    public List<TkmBinRange> getBinRanges() throws Exception {
        List<TkmBinRange> tkmBinRangeList = new ArrayList<>();
        int index = 0;
        do {
            log.info("Calling Visa bin range API. Index: " + index);
            VisaBinRangeResponse visaBinRangeResponse = invokeVisaBinRange(index);
            if (visaBinRangeResponse == null) {
                throw new EmptyResponseException("Empty Visa Response");
            }
            log.trace(visaBinRangeResponse);
            log.info(visaBinRangeResponse.getTotalRecordsCount() + " bin ranges total, this response contains " + visaBinRangeResponse.getNumRecordsReturned() + " bin ranges");
            tkmBinRangeList.addAll(getBinRangeToken(visaBinRangeResponse));
            index = getNewIndex(index, visaBinRangeResponse);
        } while (index != -1);

        return tkmBinRangeList;
    }

    private VisaBinRangeResponse invokeVisaBinRange(int index) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("keyId", keyId);
        headers.setBasicAuth(userId, password, StandardCharsets.UTF_8);

        VisaBinRangeRequestHeader requestHeader = VisaBinRangeRequestHeader.builder()
                .requestTS(Instant.now())
                .requestMessageID(UUID.randomUUID().toString())
                .build();
        VisaBinRangeRequestData requestData = new VisaBinRangeRequestData(String.valueOf(index), CHUNK_SIZE.toString());
        VisaBinRangeRequest visaBinRangeRequest = new VisaBinRangeRequest(requestHeader, requestData);
        log.trace(visaBinRangeRequest);
        HttpEntity<VisaBinRangeRequest> entity = new HttpEntity<>(visaBinRangeRequest, headers);
        return circuitBreakerService.getVisaBinRanges(retrieveBinRangesUrl, entity, restTemplate);
    }

    private int getNewIndex(int index, VisaBinRangeResponse visaBinRangeResponse) {
        if (visaBinRangeResponse != null && visaBinRangeResponse.hasMore()
                && "CDI000".equals(visaBinRangeResponse.getResponseStatus().getStatusCode())) {
            index = index + CHUNK_SIZE;
        } else {
            index = -1;
        }
        return index;
    }

    private List<TkmBinRange> getBinRangeToken(VisaBinRangeResponse visaBinRangeResponse) {
        List<TkmBinRange> collect = new ArrayList<>();
        if (visaBinRangeResponse != null) {
            collect = visaBinRangeResponse.getResponseData().stream()
                    .filter(VisaBinRangeResponseData::isForTokens)
                    .map(VisaBinRangeResponseData::toTkmBinRange).collect(Collectors.toList());
        }
        return collect;
    }
}
