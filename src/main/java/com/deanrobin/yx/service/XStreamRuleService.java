package com.deanrobin.yx.service;

import com.deanrobin.yx.config.XConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理 X Filtered Stream 的过滤规则
 * 规则格式：from:handle1 OR from:handle2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XStreamRuleService {

    private final XConfig xConfig;
    private final BearerTokenProvider bearerTokenProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String RULES_URL = "https://api.twitter.com/2/tweets/search/stream/rules";

    /**
     * 启动时同步规则：删除旧规则，按账户列表添加新规则
     */
    public void syncRules() throws Exception {
        log.info("Syncing X stream filter rules...");
        deleteAllRules();
        addRulesFromConfig();
    }

    private void deleteAllRules() throws Exception {
        // 查询当前规则
        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(RULES_URL))
                .header("Authorization", "Bearer " + bearerTokenProvider.getBearerToken())
                .GET().build();

        HttpResponse<String> res = httpClient.send(getReq, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(res.body());
        JsonNode data = root.path("data");

        if (!data.isArray() || data.isEmpty()) {
            log.info("No existing stream rules to delete.");
            return;
        }

        List<String> ids = new ArrayList<>();
        data.forEach(rule -> ids.add(rule.path("id").asText()));

        // 批量删除
        Map<String, Object> body = Map.of("delete", Map.of("ids", ids));
        String json = objectMapper.writeValueAsString(body);

        HttpRequest delReq = HttpRequest.newBuilder()
                .uri(URI.create(RULES_URL))
                .header("Authorization", "Bearer " + bearerTokenProvider.getBearerToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();

        HttpResponse<String> delRes = httpClient.send(delReq, HttpResponse.BodyHandlers.ofString());
        log.info("Deleted {} old stream rules. Response: {}", ids.size(), delRes.body());
    }

    private void addRulesFromConfig() throws Exception {
        if (xConfig.getAccounts() == null || xConfig.getAccounts().isEmpty()) {
            log.warn("No accounts configured to monitor.");
            return;
        }

        // 构建规则：from:handle1 OR from:handle2 ...（X API 单条规则最长 512 字符）
        List<XConfig.AccountConfig> enabled = xConfig.getAccounts().stream()
                .filter(XConfig.AccountConfig::isEnabled)
                .collect(Collectors.toList());

        if (enabled.isEmpty()) {
            log.warn("No enabled accounts to monitor.");
            return;
        }

        String ruleValue = enabled.stream()
                .map(a -> "from:" + a.getHandle())
                .collect(Collectors.joining(" OR "));

        String ruleTag = "yx-monitor";

        Map<String, Object> body = Map.of(
                "add", List.of(Map.of("value", ruleValue, "tag", ruleTag))
        );
        String json = objectMapper.writeValueAsString(body);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(RULES_URL))
                .header("Authorization", "Bearer " + bearerTokenProvider.getBearerToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();

        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        log.info("Added stream rule: [{}] | Response: {}", ruleValue, res.body());
    }
}
