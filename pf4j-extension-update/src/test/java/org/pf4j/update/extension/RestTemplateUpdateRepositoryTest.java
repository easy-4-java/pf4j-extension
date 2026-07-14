package org.pf4j.update.extension;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.pf4j.update.PluginInfo;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RestTemplateUpdateRepository} 元数据加载、URL 规范化和缓存行为测试。
 */
class RestTemplateUpdateRepositoryTest {

    /**
     * 包含单个插件及相对发布地址的测试 JSON。
     */
    private static final String PLUGINS_JSON = "[{\"id\":\"demo\",\"releases\":[{\"version\":\"1.0.0\","
            + "\"date\":\"2026-01-01\",\"url\":\"demo-1.0.0.zip\"}]}]";

    /**
     * 验证远程元数据会被解析、补充仓库 ID、转换为绝对 URL 并缓存。
     */
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

    /**
     * 返回固定 JSON 并记录请求次数的 RestTemplate 测试替身。
     */
    private static class StubRestTemplate extends RestTemplate {

        /**
         * 每次请求返回的固定响应文本。
         */
        private final String response;

        /**
         * 当前累计的 HTTP 请求次数。
         */
        private int requestCount;

        /**
         * 创建固定响应的 HTTP 客户端替身。
         *
         * @param response 请求时返回的 JSON 文本
         */
        private StubRestTemplate(String response) {
            this.response = response;
        }

        /**
         * 返回固定响应并累加请求计数。
         *
         * @param url 被请求的 URL
         * @param responseType 调用方期望的响应类型
         * @param uriVariables URL 模板变量
         * @param <T> 响应类型
         * @return 转换为目标类型的固定响应
         */
        @Override
        public <T> T getForObject(String url, Class<T> responseType, Object... uriVariables) {
            requestCount++;
            return responseType.cast(response);
        }

        /**
         * 获取累计请求次数。
         *
         * @return 当前请求计数
         */
        private int getRequestCount() {
            return requestCount;
        }
    }
}
