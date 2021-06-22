package it.gov.pagopa.tkm.ms.acquirermanager.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ObjectMapperUtils {
    @Autowired
    private ObjectMapper mapper;

    public String toJsonOrNull(Object o) {
        String json = null;
        try {
            json = mapper.writeValueAsString(o);
        } catch (JsonProcessingException ignore) {
        }
        return json;
    }
}
