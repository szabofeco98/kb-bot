package com.szabofeco.kb_bot.common;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class RagPropertiesTest {

    @Configuration
    @EnableConfigurationProperties(RagProperties.class)
    static class Config {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Config.class);

    @Test
    void defaultsApplyWithoutAnyConfiguration() {
        runner.run(context -> assertThat(context.getBean(RagProperties.class))
                .isEqualTo(new RagProperties(5, 0.5, new RagProperties.Chunk(500))));
    }

    @Test
    void valuesCanBeOverridden() {
        runner.withPropertyValues("rag.top-k=8", "rag.similarity-threshold=0.65", "rag.chunk.max-tokens=300")
                .run(context -> assertThat(context.getBean(RagProperties.class))
                        .isEqualTo(new RagProperties(8, 0.65, new RagProperties.Chunk(300))));
    }

    @Test
    void anOutOfRangeThresholdFailsAtStartup() {
        runner.withPropertyValues("rag.similarity-threshold=1.5")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void anInvalidNestedChunkSizeFailsAtStartup() {
        runner.withPropertyValues("rag.chunk.max-tokens=0")
                .run(context -> assertThat(context).hasFailed());
    }
}
