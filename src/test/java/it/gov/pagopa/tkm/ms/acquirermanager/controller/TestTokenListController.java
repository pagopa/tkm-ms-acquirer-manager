package it.gov.pagopa.tkm.ms.acquirermanager.controller;

import it.gov.pagopa.tkm.ms.acquirermanager.config.*;
import it.gov.pagopa.tkm.ms.acquirermanager.controller.impl.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.*;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.http.*;
import org.springframework.http.converter.*;
import org.springframework.http.converter.json.*;
import org.springframework.http.converter.xml.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.*;
import org.springframework.web.multipart.*;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.ApiEndpoints.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TestTokenListController {

    @InjectMocks
    private TokenListControllerImpl tokenListController;

    @Mock
    private BatchAcquirerServiceImpl batchAcquirerService;

    private MockMvc mockMvc;

    @BeforeEach
    void init() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(tokenListController)
                .setMessageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new StringHttpMessageConverter(),
                        new ResourceHttpMessageConverter(),
                        new FormHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(),
                        new Jaxb2RootElementHttpMessageConverter())
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void getPublicPgpKey_success() throws Exception {
        when(batchAcquirerService.getPublicPgpKey()).thenReturn("TEST");
        mockMvc.perform(
                get(TOKEN_LIST + PUBLIC_KEY))
                .andExpect(status().isOk())
                .andExpect(content().string("TEST"));
    }

    @Test
    void postAcquirerFile_success() throws Exception {
        when(batchAcquirerService.uploadFile(any(MultipartFile.class))).thenReturn(new TokenListUploadResponse());
        mockMvc.perform(
                multipart(TOKEN_LIST).file("file", new byte[]{100}))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

}
