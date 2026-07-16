package com.duoc.eft.bff.client;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.duoc.eft.bff.dto.InscripcionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class InscripcionesClientTests {
    @Test void reenviaJsonNormalizadoYBearer() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        InscripcionesClient client = new InscripcionesClient(builder, "http://inscripciones.test");

        server.expect(once(), requestTo("http://inscripciones.test/api/inscripciones"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-de-prueba"))
                .andExpect(content().json("{\"cursoId\":1,\"simularError\":false}"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.crear(new InscripcionRequest(1L, false), "Bearer token-de-prueba");
        server.verify();
    }
}
