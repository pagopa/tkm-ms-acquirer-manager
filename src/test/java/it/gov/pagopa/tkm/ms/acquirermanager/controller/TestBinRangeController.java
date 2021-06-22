package it.gov.pagopa.tkm.ms.acquirermanager.controller;

import com.fasterxml.jackson.databind.*;
import it.gov.pagopa.tkm.ms.acquirermanager.config.*;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.controller.impl.*;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.http.converter.*;
import org.springframework.http.converter.json.*;
import org.springframework.http.converter.xml.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.*;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.ApiEndpoints.LINK;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class TestBinRangeController {

    @InjectMocks
    private BinRangeControllerImpl binRangeController;

    @Mock
    private BinRangeServiceImpl binRangeHashService;

    private final ObjectMapper mapper = new ObjectMapper();

    private DefaultBeans testBeans;

    private MockMvc mockMvc;

    @BeforeEach
    void init() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(binRangeController)
                .setMessageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new StringHttpMessageConverter(),
                        new ResourceHttpMessageConverter(),
                        new FormHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(),
                        new Jaxb2RootElementHttpMessageConverter())
                .setControllerAdvice(new ErrorHandler())
                .build();
        testBeans = new DefaultBeans();
    }

    @Test
    void givenValidBinRangeRequest_returnBinRangeResponse() throws Exception {
        when(binRangeHashService.getSasLinkResponse(BatchEnum.BIN_RANGE_GEN)).thenReturn(testBeans.LINKS_RESPONSE);
        mockMvc.perform(
                get(ApiEndpoints.BIN_RANGE_BASE_PATH + LINK))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(testBeans.LINKS_RESPONSE)));
    }

}
