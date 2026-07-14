package com.duoc.guia_despacho;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.security.oauth2.resourceserver.jwt.issuer-uri=https://login.example.com/test/v2.0/",
		"app.security.jwk-set-uri=https://login.example.com/test/discovery/v2.0/keys",
		"app.security.audience=cliente-test",
		"app.security.roles-claim=roles",
		"spring.rabbitmq.listener.simple.auto-startup=false",
		"management.health.rabbit.enabled=false"
})
class GuiaDespachoApplicationTests {

	@MockBean
	private JwtDecoder jwtDecoder;

	@Test
	void contextLoads() {
	}

}
