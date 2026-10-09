package com.proveedores.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestTracingFilterTest {

    private final RequestTracingFilter filter = new RequestTracingFilter();

    @ParameterizedTest
    @ValueSource(strings = {"550e8400-e29b-41d4-a716-446655440000", "museo_request-42.v1"})
    void conservaRequestIdSeguroEnHeaderYMdc(String requestId) throws Exception {
        Result result = execute(requestId);

        assertThat(result.responseId()).isEqualTo(requestId);
        assertThat(result.mdcId()).isEqualTo(requestId);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "id\r\ninyectado", "control\u0001", "unicode-á", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void reemplazaRequestIdAusenteOViolatorioPorUuidSeguro(String requestId) throws Exception {
        Result result = execute(requestId);

        assertThat(result.responseId()).matches("[0-9a-f-]{36}");
        assertThat(result.mdcId()).isEqualTo(result.responseId());
        assertThat(result.responseId()).doesNotContain("\r", "\n");
    }

    @Test
    void limpiaMdcAlFinalizarRequest() throws Exception {
        execute("request-ok");

        assertThat(MDC.get("requestId")).isNull();
    }

    private Result execute(String requestId) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        if (requestId != null) request.addHeader(RequestTracingFilter.REQUEST_ID_HEADER, requestId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcId = new AtomicReference<>();
        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> mdcId.set(MDC.get("requestId")));
        return new Result(response.getHeader(RequestTracingFilter.REQUEST_ID_HEADER), mdcId.get());
    }

    private record Result(String responseId, String mdcId) {
    }
}
