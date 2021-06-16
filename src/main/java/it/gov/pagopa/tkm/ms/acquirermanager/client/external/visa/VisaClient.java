package it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa;

import com.fasterxml.jackson.databind.*;
import it.gov.pagopa.tkm.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.model.request.*;
import it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.model.response.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.*;
import lombok.extern.log4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.core.io.*;
import org.springframework.stereotype.*;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.security.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;

@Component
@Log4j2
public class VisaClient {

    @Autowired
    private ObjectMapper mapper;

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

    private final Integer CHUNK_SIZE = 500;

    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS").withZone(ZoneId.of(TkmDatetimeConstant.DATE_TIME_TIMEZONE));

    public List<TkmBinRange> getBinRanges() throws Exception {
        List<TkmBinRange> tkmBinRangeList = new ArrayList<>();
        int index = 0;
        //TODO LIMIT? RETRY?
        while (true) {
            log.info("Calling Visa bin range API");
            VisaBinRangeResponse response = getBinRangesChunk(index);
            log.trace(response.toString());
            log.info(response.getTotalRecordsCount() + " bin ranges total, this response contains " + response.getNumRecordsReturned() + " bin ranges");
            tkmBinRangeList.addAll(response.getResponseData().stream()
                    .filter(VisaBinRangeResponseData::isForTokens)
                    .map(VisaBinRangeResponseData::toTkmBinRange).collect(Collectors.toList()));
            if (response.hasMore() && "CDI000".equals(response.getResponseStatus().getStatusCode())) {
                index = index + CHUNK_SIZE;
            } else {
                break;
            }
        }
        return tkmBinRangeList;
    }

    private VisaBinRangeResponse getBinRangesChunk(Integer index) throws Exception {
        VisaBinRangeRequestHeader requestHeader = new VisaBinRangeRequestHeader(
                dateFormat.format(Instant.now()),
                UUID.randomUUID().toString()
        );
        VisaBinRangeRequestData requestData = new VisaBinRangeRequestData(
                index.toString(), CHUNK_SIZE.toString()
        );
        VisaBinRangeRequest visaBinRangeRequest = new VisaBinRangeRequest(requestHeader, requestData);
        String reqPayload = mapper.writeValueAsString(visaBinRangeRequest);
        log.trace(reqPayload);
        return invokeAPI(reqPayload);
    }

    private VisaBinRangeResponse invokeAPI(String payload) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(retrieveBinRangesUrl).openConnection();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(publicCert.getInputStream(), keystorePassword.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, keystorePassword.toCharArray());
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);
        if (con instanceof HttpsURLConnection) {
            ((HttpsURLConnection) con).setSSLSocketFactory(sslContext.getSocketFactory());
        }
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("keyId", keyId);
        byte[] encodedAuth = Base64.getEncoder().encode((userId + ":" + password).getBytes(StandardCharsets.UTF_8));
        String authHeaderValue = "Basic " + new String(encodedAuth);
        con.setRequestProperty("Authorization", authHeaderValue);
        if (payload != null && payload.trim().length() > 0) {
            con.setDoOutput(true);
            con.setDoInput(true);
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = payload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }
        int status = con.getResponseCode();
        BufferedReader in;
        if (status == 200) {
            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        } else {
            in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            log.error("Two-Way (Mutual) SSL test failed");
        }
        String response;
        StringBuilder content = new StringBuilder();
        while ((response = in.readLine()) != null) {
            content.append(response);
        }
        in.close();
        con.disconnect();
        String responseAsString = content.toString();
        return mapper.readValue(responseAsString, VisaBinRangeResponse.class);
    }

}
