package com.proveedores.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerUploadTest {

    @Test
    void excesoMultipartDevuelve413ConContratoApiError() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/objetos/1/fotos");

        var response = handler.handleMaxUploadSizeExceeded(new MaxUploadSizeExceededException(10), request);

        assertThat(response.getStatusCode().value()).isEqualTo(413);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(413);
        assertThat(response.getBody().message()).isEqualTo("La carga supera el tamano maximo permitido");
        assertThat(response.getBody().path()).isEqualTo("/api/objetos/1/fotos");
    }
}
