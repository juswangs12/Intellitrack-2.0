package com.intellitrack.service;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Service
public class AiService {

    private final WebClient webClient;
    private final Environment environment;
    private final ConcurrentHashMap<String, String> promptCache = new ConcurrentHashMap<>();
    private final Semaphore rateLimiter = new Semaphore(2);
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    private static final String SYSTEM_PROMPT = "You are IntelliTrack Submission Evaluation AI. You analyze academic submissions. You MUST compute deadline status using submittedAt vs dueAt. You MUST return structured JSON only. You MUST NOT be generic. You MUST act as a deterministic evaluator, not a chatbot. Output format must be EXACTLY this JSON structure:\n{\n  \"submissionAnalysis\": \"detailed academic evaluation with specific observations\",\n  \"deadlineStatus\": {\n    \"status\": \"EARLY|ON_TIME|LATE|AT_RISK|UNKNOWN\",\n    \"explanation\": \"clear time comparison reasoning\"\n  },\n  \"progressEvaluation\": \"evaluation of completeness vs expected capstone stage\",\n  \"issuesFound\": [\"specific missing section 1\", \"specific weakness 2\", \"specific structure problem 3\"],\n  \"recommendations\": [\"actionable improvement 1\", \"actionable improvement 2\", \"actionable improvement 3\"],\n  \"riskLevel\": \"LOW|MEDIUM|HIGH\"\n}";

    public AiService(WebClient aiWebClient, Environment environment) {
        this.webClient = aiWebClient;
        this.environment = environment;
    }

    private String normalizeAndHashPrompt(String prompt) {
        String normalized = prompt.trim().toLowerCase().replaceAll("\\s+", " ");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(normalized.hashCode());
        }
    }

    public Mono<String> getCompletion(String prompt) {
        System.out.println("=== AiService.getCompletion called ===");
        
        String cacheKey = normalizeAndHashPrompt(prompt);
        
        if (promptCache.containsKey(cacheKey)) {
            System.out.println("=== Using cached AI response ===");
            return Mono.just(promptCache.get(cacheKey));
        }
        
        String apiKey = environment.getProperty("ai.api.key");
        String apiUrl = environment.getProperty("ai.api.url", "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent");
        String model = environment.getProperty("ai.api.model", "gemini-2.0-flash");
        
        apiUrl = apiUrl.trim().replace("`", "").replace("'", "");
        
        System.out.println("API Key is set: " + (apiKey != null && !apiKey.isEmpty()));
        System.out.println("API URL (cleaned): " + apiUrl);
        System.out.println("Model: " + model);
        
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("No API key, returning mock insight");
            String mock = generateMockInsight(prompt);
            promptCache.put(cacheKey, mock);
            return Mono.just(mock);
        }

        Map<String, Object> requestBody = Map.of(
            "systemInstruction", Map.of(
                "parts", List.of(Map.of("text", SYSTEM_PROMPT))
            ),
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))
            )
        );

        String fullUrl;
        if (apiUrl.contains("?")) {
            fullUrl = apiUrl + "&key=" + apiKey;
        } else {
            fullUrl = apiUrl + "?key=" + apiKey;
        }
        
        System.out.println("Full API URL (key hidden): " + fullUrl.replace(apiKey, "HIDDEN"));

        return Mono.fromCallable(() -> {
            try {
                rateLimiter.acquire();
                return webClient.post()
                    .uri(fullUrl)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .map(response -> {
                        System.out.println("=== Received AI response ===");
                        try {
                            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                            if (candidates == null || candidates.isEmpty()) {
                                System.out.println("No candidates in AI response");
                                String mock = generateMockInsight(prompt);
                                promptCache.put(cacheKey, mock);
                                return mock;
                            }
                            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                            if (content == null) {
                                System.out.println("No content in AI response");
                                String mock = generateMockInsight(prompt);
                                promptCache.put(cacheKey, mock);
                                return mock;
                            }
                            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                            if (parts == null || parts.isEmpty()) {
                                System.out.println("No parts in AI response");
                                String mock = generateMockInsight(prompt);
                                promptCache.put(cacheKey, mock);
                                return mock;
                            }
                            String result = (String) parts.get(0).get("text");
                            System.out.println("AI Response parsed successfully");
                            promptCache.put(cacheKey, result);
                            return result;
                        } catch (Exception e) {
                            System.err.println("Error parsing AI response: " + e.getMessage());
                            String mock = generateMockInsight(prompt);
                            promptCache.put(cacheKey, mock);
                            return mock;
                        }
                    })
                    .onErrorResume(e -> {
                        System.err.println("=== AI service error (using fallback) ===");
                        System.err.println("Error message: " + e.getMessage());
                        String mock = generateMockInsight(prompt);
                        promptCache.put(cacheKey, mock);
                        return Mono.just(mock);
                    })
                    .block();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                String mock = generateMockInsight(prompt);
                promptCache.put(cacheKey, mock);
                return mock;
            } finally {
                rateLimiter.release();
            }
        }).subscribeOn(reactor.core.scheduler.Schedulers.fromExecutorService(executorService)).flatMap(Mono::justOrEmpty);
    }

    private String generateMockInsight(String prompt) {
        return "{\"submissionAnalysis\":\"Submission analyzed using fallback evaluator.\",\"deadlineStatus\":{\"status\":\"UNKNOWN\",\"explanation\":\"No timestamp data available for deadline calculation.\"},\"progressEvaluation\":\"Completeness cannot be determined with available data.\",\"issuesFound\":[\"Data insufficient for detailed critique\"],\"recommendations\":[\"Ensure submission metadata is provided\"],\"riskLevel\":\"LOW\"}";
    }
}
