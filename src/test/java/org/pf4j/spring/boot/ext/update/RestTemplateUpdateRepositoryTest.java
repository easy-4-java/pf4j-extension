package org.pf4j.spring.boot.ext.update;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.pf4j.update.PluginInfo;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestTemplateUpdateRepositoryTest {

    private static final String PLUGINS_JSON = "[{\"id\":\"demo\",\"releases\":[{\"version\":\"1.0.0\","
            + "\"date\":\"2026-01-01\",\"url\":\"demo-1.0.0.zip\"}]}]";

    @Test
    void shouldLoadNormalizeAndCachePluginMetadata() {
        StubRestTemplate restTemplate = new StubRestTemplate(PLUGINS_JSON);
        RestTemplateUpdateRepository repository = new RestTemplateUpdateRepository("repo",
                "https://plugins.example.org/", restTemplate);

        Map<String, PluginInfo> plugins = repository.getPlugins();

        assertTrue(plugins.containsKey("demo"));
        assertEquals("repo", plugins.get("demo").getRepositoryId());
        assertEquals("https://plugins.example.org/demo-1.0.0.zip",
                plugins.get("demo").releases.get(0).url);
        assertSame(plugins, repository.getPlugins());
        assertEquals(1, restTemplate.getRequestCount());
    }

    private static class StubRestTemplate extends RestTemplate {

        private final String response;
        private int requestCount;

        private StubRestTemplate(String response) {
            this.response = response;
        }

        @Override
        public <T> T getForObject(String url, Class<T> responseType, Object... uriVariables) {
            requestCount++;
            return responseType.cast(response);
        }

        private int getRequestCount() {
            return requestCount;
        }
    }
}
