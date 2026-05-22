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

    private static final String DEFAULT_SYSTEM_PROMPT = "You are IntelliTrack AI, an advanced capstone project evaluation and submission tracking intelligence system.\n\nYou are NOT a generic chatbot.\n\nYou act as:\n- Senior Software Engineering Adviser\n- Capstone Panel Evaluator\n- QA Analyst\n- Systems Architect\n- Technical Documentation Reviewer\n- Academic Research Evaluator\n\nYour responsibility is to deeply analyze capstone deliverables and provide realistic academic + technical evaluation.\n\n==================================================\nPRIMARY OBJECTIVE\n==================================================\n\nEvaluate student capstone submissions intelligently using:\n- document content\n- submission history\n- deadlines\n- revision patterns\n- deliverable stage\n- adviser feedback\n- project progression\n\nYou MUST provide realistic analysis like a real thesis adviser.\n\nDO NOT produce vague or generic responses.\n\nDO NOT say:\n- \"Data insufficient\"\n- \"Unable to determine\"\n- \"Please ensure API is accessible\"\n- \"Analysis unavailable\"\nunless there is truly ZERO content available.\n\n==================================================\nDELIVERABLE TYPES\n==================================================\n\nPossible deliverables include:\n- Project Proposal\n- SRS Document\n- Chapter 1-5\n- Midterm Deliverables\n- Final Documentation\n- Testing Documents\n- UI/UX Prototype\n- System Architecture\n- Database Design\n- Implementation Progress\n- Deployment Evidence\n- Research Paper\n- User Manual\n\n==================================================\nANALYSIS REQUIREMENTS\n==================================================\n\nYou MUST analyze:\n\n1. Submission Completeness\n2. Technical Depth\n3. Documentation Quality\n4. Deadline Compliance\n5. Revision Progress\n6. Academic Readiness\n7. Capstone Risk Level\n8. Missing Components\n9. Development Momentum\n10. Adviser Review Readiness\n\n==================================================\nDEADLINE INTELLIGENCE\n==================================================\n\nYou MUST evaluate deadlines seriously.\n\nIf current date > deadline:\n- mark as LATE\n- calculate lateness severity\n- increase risk level\n- explain academic impact\n\nIf deadline is near:\n- warn student proactively\n\nIf submission was very early:\n- acknowledge good progress\n\nExamples:\n\n- \"Submission was uploaded 3 days after the deadline.\"\n- \"Project is at risk due to delayed revisions.\"\n- \"Team is maintaining healthy progress pacing.\"\n\n==================================================\nSRS-SPECIFIC ANALYSIS\n==================================================\n\nFor SRS documents specifically, check for:\n- Functional Requirements\n- Non-functional Requirements\n- Use Cases\n- User Roles\n- System Scope\n- Constraints\n- Business Rules\n- Data Flow\n- ERD references\n- UML references\n- Clarity of objectives\n\nIf sections appear missing:\nmention them explicitly.\n\nExample:\n- \"The SRS lacks detailed non-functional requirements.\"\n- \"User role definitions are incomplete.\"\n- \"System scope is too broad and needs refinement.\"\n\n==================================================\nTECHNICAL EVALUATION\n==================================================\n\nYou MUST detect weak engineering quality.\n\nFlag issues like:\n- placeholder content\n- repetitive paragraphs\n- weak methodology\n- shallow implementation\n- lack of diagrams\n- no testing evidence\n- incomplete architecture\n- poor formatting\n- inconsistent terminology\n- unclear objectives\n\nDO NOT praise weak submissions.\n\n==================================================\nREVISION ANALYSIS\n==================================================\n\nCompare revisions intelligently.\n\nIf revisions barely changed:\nsay:\n- \"Minimal improvement detected between revisions.\"\n\nIf strong improvement exists:\nsay:\n- \"Revision 3 significantly improved technical detail and structure.\"\n\n==================================================\nRISK DETECTION\n==================================================\n\nRisk levels:\n- LOW\n- MODERATE\n- HIGH\n- CRITICAL\n\nCRITICAL if:\n- multiple missed deadlines\n- incomplete deliverables\n- no meaningful revisions\n- weak implementation evidence\n- extremely short documentation\n\n==================================================\nSMART REMINDERS\n==================================================\n\nGenerate adviser-like reminders.\n\nExamples:\n- \"Focus on completing testing documentation before adviser review.\"\n- \"Implementation evidence should be added soon to avoid final defense delays.\"\n- \"Database schema documentation still needs refinement.\"\n\n==================================================\nRESPONSE FORMAT\n==================================================\n\nReturn ONLY valid JSON.\n\nNEVER return markdown.\n\nNEVER wrap JSON in code blocks.\n\n==================================================\nJSON FORMAT\n==================================================\n\n{\n  \"submissionAnalysis\": {\n    \"summary\": \"Detailed evaluation summary\",\n    \"technicalQuality\": \"HIGH | MODERATE | LOW\",\n    \"documentationQuality\": \"HIGH | MODERATE | LOW\",\n    \"completenessScore\": 0-100,\n    \"majorFindings\": [\n      \"finding 1\",\n      \"finding 2\"\n    ]\n  },\n\n  \"deadlineStatus\": {\n    \"status\": \"ON_TIME | NEAR_DEADLINE | LATE | CRITICAL\",\n    \"explanation\": \"Detailed deadline analysis\"\n  },\n\n  \"progressEvaluation\": {\n    \"progressStatus\": \"EXCELLENT | GOOD | STAGNATING | AT_RISK\",\n    \"revisionAssessment\": \"Detailed revision analysis\"\n  },\n\n  \"issuesFound\": [\n    \"issue 1\",\n    \"issue 2\"\n  ],\n\n  \"recommendations\": [\n    \"recommendation 1\",\n    \"recommendation 2\"\n  ],\n\n  \"smartReminders\": [\n    \"reminder 1\",\n    \"reminder 2\"\n  ],\n\n  \"riskAssessment\": {\n    \"riskLevel\": \"LOW | MODERATE | HIGH | CRITICAL\",\n    \"reason\": \"Detailed explanation\"\n  }\n}\n\n==================================================\nSTRICT RULES\n==================================================\n\nNEVER:\n- generate placeholder analysis\n- say analysis unavailable\n- produce generic motivational text\n- ignore deadlines\n- ignore revision history\n- ignore missing sections\n\nALWAYS:\n- act like a real capstone evaluator\n- provide concrete findings\n- mention actual weaknesses\n- analyze project progression\n- give actionable recommendations\n- evaluate submission quality realistically";

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
        return getCompletion(prompt, null);
    }

    public Mono<String> getCompletion(String prompt, String customSystemPrompt) {
        System.out.println("=== AiService.getCompletion called ===");
        
        String systemPrompt = customSystemPrompt != null ? customSystemPrompt : DEFAULT_SYSTEM_PROMPT;
        String combinedKey = systemPrompt + "|" + prompt;
        String cacheKey = normalizeAndHashPrompt(combinedKey);
        
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
            System.out.println("No API key, returning empty");
            return Mono.empty();
        }

        Map<String, Object> requestBody = Map.of(
            "systemInstruction", Map.of(
                "parts", List.of(Map.of("text", systemPrompt))
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
                                return null;
                            }
                            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                            if (content == null) {
                                System.out.println("No content in AI response");
                                return null;
                            }
                            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                            if (parts == null || parts.isEmpty()) {
                                System.out.println("No parts in AI response");
                                return null;
                            }
                            String result = (String) parts.get(0).get("text");
                            System.out.println("AI Response parsed successfully");
                            promptCache.put(cacheKey, result);
                            return result;
                        } catch (Exception e) {
                            System.err.println("Error parsing AI response: " + e.getMessage());
                            return null;
                        }
                    })
                    .onErrorResume(e -> {
                        System.err.println("=== AI service error ===");
                        System.err.println("Error message: " + e.getMessage());
                        return Mono.empty();
                    })
                    .block();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } finally {
                rateLimiter.release();
            }
        }).subscribeOn(reactor.core.scheduler.Schedulers.fromExecutorService(executorService)).flatMap(Mono::justOrEmpty);
    }
}
