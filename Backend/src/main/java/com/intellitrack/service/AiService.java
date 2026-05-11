package com.intellitrack.service;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private final WebClient webClient;
    private final Environment environment;

    public AiService(WebClient aiWebClient, Environment environment) {
        this.webClient = aiWebClient;
        this.environment = environment;
    }

    public Mono<String> getCompletion(String prompt) {
        System.out.println("=== AiService.getCompletion called ===");
        
        String apiKey = environment.getProperty("ai.api.key");
        String apiUrl = environment.getProperty("ai.api.url", "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent");
        String model = environment.getProperty("ai.api.model", "gemini-2.0-flash");
        
        // Clean up API URL: remove any whitespace or backticks
        apiUrl = apiUrl.trim().replace("`", "").replace("'", "");
        
        System.out.println("API Key is set: " + (apiKey != null && !apiKey.isEmpty()));
        System.out.println("API URL (cleaned): " + apiUrl);
        System.out.println("Model: " + model);
        
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("No API key, returning mock insight");
            return Mono.just(generateMockInsight(prompt));
        }

        // Gemini API request structure
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", "System Instruction: You are an AI assistant for IntelliTrack, a capstone tracking system. Provide concise and professional insights.\n\nUser Prompt: " + prompt)
                ))
            )
        );

        // Build full URL with API key
        String fullUrl;
        if (apiUrl.contains("?")) {
            fullUrl = apiUrl + "&key=" + apiKey;
        } else {
            fullUrl = apiUrl + "?key=" + apiKey;
        }
        
        System.out.println("Full API URL (key hidden): " + fullUrl.replace(apiKey, "HIDDEN"));

        return webClient.post()
            .uri(fullUrl)
            .header("Content-Type", "application/json")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> {
                System.out.println("=== Received AI response ===");
                try {
                    // Gemini response parsing: candidates[0].content.parts[0].text
                    List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                    if (candidates == null || candidates.isEmpty()) {
                        System.out.println("No candidates in AI response");
                        return generateMockInsight(prompt);
                    }
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    if (content == null) {
                        System.out.println("No content in AI response");
                        return generateMockInsight(prompt);
                    }
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (parts == null || parts.isEmpty()) {
                        System.out.println("No parts in AI response");
                        return generateMockInsight(prompt);
                    }
                    String result = (String) parts.get(0).get("text");
                    System.out.println("AI Response parsed successfully");
                    return result;
                } catch (Exception e) {
                    System.err.println("Error parsing AI response: " + e.getMessage());
                    return generateMockInsight(prompt);
                }
            })
            .onErrorResume(e -> {
                System.err.println("=== AI service error (using fallback) ===");
                System.err.println("Error message: " + e.getMessage());
                return Mono.just(generateMockInsight(prompt));
            });
    }

    private String generateMockInsight(String prompt) {
        if (prompt.contains("student progress")) {
            return "Based on your current submission frequency, you are maintaining a steady pace. Focus on finalizing Chapter 3 to stay ahead of the next milestone.";
        } else if (prompt.contains("Adviser dashboard summary")) {
            return "Your assigned groups are currently showing 85% activity. Group Alpha requires immediate attention due to a pending revision request for their SRS document.";
        } else if (prompt.contains("Coordinator system summary")) {
            return "Institutional progress is currently at 72%. Late submissions have decreased by 15% this week, but technical manual deliverables remain a potential bottleneck.";
        } else if (prompt.contains("predict academic workflow risk")) {
            return "Workflow analysis predicts a high probability of on-time completion for this deliverable based on the group's historical revision consistency.";
        } else if (prompt.contains("strategic overview")) {
            return "System-wide analytics suggest optimizing the adviser review window to improve overall turnaround time for midterm documentation.";
        } else if (prompt.contains("CONCISE SUMMARY")) {
            return "SUMMARY: This submission shows solid progress on the deliverable with clear objectives outlined.\nOBJECTIVES: Complete research; Finalize methodology\nHIGHLIGHTS: Well-structured introduction; Clear research questions; Comprehensive literature review\nMISSING: Detailed timeline for implementation";
        }
        return "AI insights are temporarily unavailable. Your work is being tracked normally.";
    }
}
