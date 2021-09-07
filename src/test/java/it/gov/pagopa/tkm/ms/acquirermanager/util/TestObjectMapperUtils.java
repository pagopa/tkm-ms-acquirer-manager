package it.gov.pagopa.tkm.ms.acquirermanager.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.Token;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class TestObjectMapperUtils {
    @InjectMocks
    private ObjectMapperUtils mapperUtils;

    @Spy
    private ObjectMapper mapper;

    @Test
    void toJsonOrNull_json() {
        String tokenValue = "token";
        String hTokenValue = "hToken";
        Token token = Token.builder().token(tokenValue).hToken(hTokenValue).build();
        String json = mapperUtils.toJsonOrNull(token);
        Assertions.assertEquals("{\"token\":\"" + tokenValue + "\",\"htoken\":\"" + hTokenValue + "\"}", json);
    }

    @Test
    void toJsonOrNull_null() throws JsonProcessingException {
        String tokenValue = "token";
        String hTokenValue = "hToken";
        Token token = Token.builder().token(tokenValue).hToken(hTokenValue).build();
        Mockito.when(mapper.writeValueAsString(Mockito.any())).thenThrow(new JsonProcessingException("") {
        });
        String json = mapperUtils.toJsonOrNull(token);
        Assertions.assertNull(json);
    }
}