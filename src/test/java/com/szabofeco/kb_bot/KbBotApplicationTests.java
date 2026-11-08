package com.szabofeco.kb_bot;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "spring.autoconfigure.exclude="
		+ "org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreAutoConfiguration")
class KbBotApplicationTests {

	// The Chroma auto-config is disabled, so we mock the VectorStore to let the context start up.
	@MockitoBean
	VectorStore vectorStore;

	@Test
	void contextLoads() {
	}

}
