package com.claimwise.service;

import com.claimwise.client.ExternalLlmClient;
import com.claimwise.dto.AiReviewResponse;
import com.claimwise.dto.ValidationIssue;
import com.claimwise.dto.ValidationResult;
import com.claimwise.entity.AiReview;
import com.claimwise.entity.ExpenseClaim;
import com.claimwise.entity.Policy;
import com.claimwise.exception.ResourceNotFoundException;
import com.claimwise.repository.AiReviewRepository;
import com.claimwise.repository.ExpenseClaimRepository;
import com.claimwise.repository.PolicyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIReviewService {

    private static final Set<String> ALLOWED_CLASSIFICATIONS = Set.of(
            "COMPLIANT", "NEEDS_CLARIFICATION", "NEEDS_REVIEW", "NON_COMPLIANT"
    );

    private static final Set<String> ALLOWED_ACTIONS = Set.of(
            "APPROVE", "REJECT", "REQUEST_CLARIFICATION", "FLAG_FOR_AUDIT"
    );

    private final ExpenseClaimRepository claimRepository;
    private final PolicyRepository policyRepository;
    private final AiReviewRepository aiReviewRepository;
    private final DeterministicValidationService validationService;
    private final AuditLogService auditLogService;
    private final ExternalLlmClient externalLlmClient;
    private final ObjectMapper objectMapper;

    public Optional<AiReviewResponse> getStoredAiReview(Long claimId) {
        return aiReviewRepository.findByClaimId(claimId)
                .map(AiReviewResponse::fromEntity);
    }

    @Transactional
    public AiReviewResponse generateAiReview(Long claimId) {
        ExpenseClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense Claim", "id", claimId));

        auditLogService.recordSystemEvent(
                claimId,
                "AI_REVIEW_STARTED",
                "AI policy analysis evaluation initiated for category " + claim.getCategory(),
                "Amount: " + claim.getAmount() + " " + claim.getCurrency()
        );

        List<Policy> relevantPolicies = policyRepository.findByCategoryAndActiveTrue(claim.getCategory());
        ValidationResult valResult = validationService.validateClaim(claim);

        String policyEvidenceText = relevantPolicies.isEmpty()
                ? "No specific policy rule configured for category " + claim.getCategory() + ". Standard organizational policy applies."
                : relevantPolicies.stream()
                .map(p -> String.format("[%s: %s] Max Allowed: %s %s | Receipt Required: %s | Rules: %s",
                        p.getPolicyCode(), p.getTitle(), p.getMaximumAmount(), claim.getCurrency(), p.isReceiptRequired(), p.getRuleText()))
                .collect(Collectors.joining("\n"));

        AiReviewAnalysis analysis;
        String providerMetadata;
        boolean usedFallback = false;

        if (externalLlmClient.isApiKeyConfigured()) {
            try {
                analysis = tryExternalLlmReview(claim, relevantPolicies, valResult, policyEvidenceText);
                providerMetadata = "EXTERNAL_LLM (" + externalLlmClient.getProviderInfo() + ")";
            } catch (Exception e) {
                log.warn("External LLM execution failed for claim #{}. Falling back to secondary structured analysis engine: {}", claimId, e.getMessage());
                analysis = performStructuredAnalysis(claim, relevantPolicies, valResult, policyEvidenceText, "LLM_ERROR: " + e.getMessage());
                providerMetadata = "FALLBACK_ENGINE (Reason: External LLM execution failed - " + e.getMessage() + ")";
                usedFallback = true;
            }
        } else {
            log.info("AI API key not configured for claim #{}. Executing secondary structured analysis engine.", claimId);
            analysis = performStructuredAnalysis(claim, relevantPolicies, valResult, policyEvidenceText, "MISSING_API_KEY");
            providerMetadata = "FALLBACK_ENGINE (Reason: AI API key not configured)";
            usedFallback = true;
        }

        // Save or update AiReview entity in DB (Note: AI output never mutates claim.status directly)
        AiReview review = aiReviewRepository.findByClaimId(claimId)
                .orElse(AiReview.builder().claim(claim).build());

        review.setClassification(analysis.classification());
        review.setConfidence(analysis.confidence());
        review.setUncertain(analysis.uncertain());
        review.setSummary(analysis.summary());
        review.setReasoning(analysis.reasoning());
        review.setMissingInformation(analysis.missingInformation());
        review.setPolicyEvidence(analysis.policyEvidence());
        review.setRecommendedAction(analysis.recommendedAction());

        AiReview savedReview = aiReviewRepository.save(review);

        String eventAction = usedFallback ? "AI_REVIEW_FALLBACK_USED" : "AI_REVIEW_COMPLETED";
        String eventDesc = usedFallback
                ? "AI policy review completed using Fallback Engine. Classification: " + analysis.classification()
                : "AI policy review completed using External LLM. Classification: " + analysis.classification();

        auditLogService.recordSystemEvent(
                claimId,
                eventAction,
                eventDesc,
                "Provider: " + providerMetadata + " | Confidence: " + Math.round(analysis.confidence() * 100) + "% | Uncertain: " + analysis.uncertain()
        );

        return AiReviewResponse.fromEntity(savedReview);
    }

    private AiReviewAnalysis tryExternalLlmReview(
            ExpenseClaim claim,
            List<Policy> relevantPolicies,
            ValidationResult valResult,
            String policyEvidenceText
    ) throws Exception {

        String systemPrompt = """
                You are an expert enterprise Expense Claim Policy Assistant. Your task is to evaluate expense claims against organizational policy.
                IMPORTANT ADVISORY RULES:
                1. Your evaluation is strictly advisory to assist human reviewers. You MUST NOT make final decisions (do not alter claim approval state).
                2. Return ONLY a valid JSON object matching the requested schema exactly.
                3. Ground all policy citations exclusively in the provided DB Policy Evidence. Do NOT invent policy codes or titles.
                4. Indicate uncertainty (uncertain = true) if confidence is low (< 0.75) or information is ambiguous.
                """;

        String userPrompt = String.format("""
                EXPENSE CLAIM DETAILS:
                - Claim ID: %d
                - Claimant ID: %s
                - Date: %s
                - Category: %s
                - Amount: %s %s
                - Description: %s
                - Receipt Available: %b

                DETERMINISTIC VALIDATION FINDINGS:
                - Status: %s
                - Issues: %s

                GROUNDED DATABASE POLICY EVIDENCE:
                %s

                OUTPUT SCHEMA REQUIREMENTS:
                Return a JSON object with EXACTLY the following structure:
                {
                  "classification": "COMPLIANT | NEEDS_CLARIFICATION | NEEDS_REVIEW | NON_COMPLIANT",
                  "confidence": 0.85,
                  "uncertain": false,
                  "summary": "Brief 1-2 sentence summary of finding",
                  "reasoning": "Detailed breakdown of compliance analysis",
                  "missingInformation": ["List of missing documentation if any"],
                  "policyEvidence": ["Policy codes or titles cited from DB evidence"],
                  "recommendedAction": "APPROVE | REJECT | REQUEST_CLARIFICATION | FLAG_FOR_AUDIT"
                }
                """,
                claim.getId(),
                claim.getClaimant() != null ? claim.getClaimant().getEmail() : "Unknown",
                claim.getExpenseDate(),
                claim.getCategory(),
                claim.getAmount(),
                claim.getCurrency(),
                claim.getDescription(),
                claim.isReceiptAvailable(),
                valResult.getStatus(),
                valResult.getIssues().stream().map(ValidationIssue::getMessage).collect(Collectors.joining("; ")),
                policyEvidenceText
        );

        String rawResponse = externalLlmClient.generateReviewAnalysis(systemPrompt, userPrompt);
        return parseAndValidateLlmResponse(rawResponse, relevantPolicies);
    }

    private AiReviewAnalysis parseAndValidateLlmResponse(String rawResponse, List<Policy> relevantPolicies) throws Exception {
        String cleanJson = rawResponse.trim();
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.substring(7);
        } else if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.substring(3);
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
        }
        cleanJson = cleanJson.trim();

        JsonNode rootNode = objectMapper.readTree(cleanJson);

        // Required field & Enum validation
        String classification = rootNode.path("classification").asText("").toUpperCase();
        if (!ALLOWED_CLASSIFICATIONS.contains(classification)) {
            throw new IllegalArgumentException("Invalid or missing classification in AI response: " + classification);
        }

        if (!rootNode.has("confidence") || rootNode.get("confidence").isNull()) {
            throw new IllegalArgumentException("Missing confidence score in AI response");
        }
        double confidence = rootNode.get("confidence").asDouble(-1.0);
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence score out of bounds (must be 0.0-1.0): " + confidence);
        }

        boolean uncertain = rootNode.path("uncertain").asBoolean(confidence < 0.75);
        String summary = rootNode.path("summary").asText("").trim();
        String reasoning = rootNode.path("reasoning").asText("").trim();
        String recommendedAction = rootNode.path("recommendedAction").asText("").toUpperCase();

        if (!ALLOWED_ACTIONS.contains(recommendedAction)) {
            throw new IllegalArgumentException("Invalid or missing recommended action: " + recommendedAction);
        }

        if (summary.isBlank() || reasoning.isBlank()) {
            throw new IllegalArgumentException("Summary and reasoning cannot be empty in AI response");
        }

        // Parse missing information
        List<String> missingList = new ArrayList<>();
        JsonNode missingNode = rootNode.path("missingInformation");
        if (missingNode.isArray()) {
            missingNode.forEach(n -> { if (!n.asText().isBlank()) missingList.add(n.asText().trim()); });
        } else if (!missingNode.isMissingNode() && !missingNode.asText().isBlank()) {
            missingList.add(missingNode.asText().trim());
        }
        String missingInfoStr = String.join("; ", missingList);

        // Database Grounded Policy Evidence Validation (GAP 4)
        List<String> rawCitations = new ArrayList<>();
        JsonNode policyNode = rootNode.path("policyEvidence");
        if (policyNode.isArray()) {
            policyNode.forEach(n -> { if (!n.asText().isBlank()) rawCitations.add(n.asText().trim()); });
        } else if (!policyNode.isMissingNode() && !policyNode.asText().isBlank()) {
            rawCitations.add(policyNode.asText().trim());
        }

        List<String> validatedCitations = new ArrayList<>();
        for (String citation : rawCitations) {
            boolean matchesDbPolicy = relevantPolicies.isEmpty() || relevantPolicies.stream().anyMatch(p ->
                    citation.toLowerCase().contains(p.getPolicyCode().toLowerCase()) ||
                    citation.toLowerCase().contains(p.getTitle().toLowerCase()) ||
                    p.getRuleText().toLowerCase().contains(citation.toLowerCase()) ||
                    p.getPolicyCode().toLowerCase().contains(citation.toLowerCase())
            );

            if (matchesDbPolicy) {
                validatedCitations.add(citation);
            } else {
                log.warn("Discarding unsupported policy citation invented by LLM: {}", citation);
            }
        }

        if (!relevantPolicies.isEmpty() && validatedCitations.isEmpty()) {
            uncertain = true;
            validatedCitations.add("Standard Organizational Policy Guidelines");
        } else if (relevantPolicies.isEmpty()) {
            validatedCitations.add("Standard Organizational Policy Guidelines");
        }

        String policyEvidenceStr = String.join("; ", validatedCitations);

        return new AiReviewAnalysis(
                classification,
                confidence,
                uncertain,
                summary,
                reasoning,
                missingInfoStr,
                policyEvidenceStr,
                recommendedAction
        );
    }

    private AiReviewAnalysis performStructuredAnalysis(
            ExpenseClaim claim,
            List<Policy> relevantPolicies,
            ValidationResult valResult,
            String policyEvidenceText,
            String fallbackReason
    ) {
        String classification;
        double confidence;
        boolean uncertain = false;
        StringBuilder reasoning = new StringBuilder("[FALLBACK ENGINE: " + fallbackReason + "] ");
        StringBuilder missingInfo = new StringBuilder();
        String recommendedAction;

        boolean isDeterministicFailed = "FAILED".equalsIgnoreCase(valResult.getStatus());
        boolean hasMissingReceiptIssue = valResult.getIssues().stream().anyMatch(i -> "MISSING_RECEIPT".equals(i.getType()));

        if (isDeterministicFailed) {
            classification = "NON_COMPLIANT";
            confidence = 0.95;
            recommendedAction = "REJECT";
            reasoning.append("Deterministic backend validation failed due to policy violations: ");
            for (ValidationIssue issue : valResult.getIssues()) {
                reasoning.append(issue.getMessage()).append(" ");
            }

            if (hasMissingReceiptIssue) {
                missingInfo.append("Itemized physical receipt or tax invoice is required for verification. ");
            }
        } else if ("WARNING".equalsIgnoreCase(valResult.getStatus())) {
            classification = "NEEDS_REVIEW";
            confidence = 0.70;
            uncertain = true;
            recommendedAction = "FLAG_FOR_AUDIT";
            reasoning.append("Claim contains potential policy warnings requiring manual reviewer inspection: ");
            for (ValidationIssue issue : valResult.getIssues()) {
                reasoning.append(issue.getMessage()).append(" ");
            }
        } else {
            String desc = claim.getDescription().toLowerCase();
            boolean isVague = desc.contains("stuff") || desc.contains("misc") || desc.contains("items") || desc.length() < 15;

            if (isVague) {
                classification = "NEEDS_CLARIFICATION";
                confidence = 0.65;
                uncertain = true;
                recommendedAction = "REQUEST_CLARIFICATION";
                reasoning.append("The expense description appears ambiguous or vague ('").append(claim.getDescription()).append("'). Detailed business rationale is requested.");
                missingInfo.append("Specific business purpose, attendee list, or itemized receipt breakdown.");
            } else {
                classification = "COMPLIANT";
                confidence = 0.90;
                recommendedAction = "APPROVE";
                reasoning.append("The claim fully satisfies organizational policy guidelines for ").append(claim.getCategory())
                        .append(". Amount (").append(claim.getAmount()).append(" ").append(claim.getCurrency())
                        .append(") is within allowed threshold and receipt criteria are satisfied.");
            }
        }

        String citedEvidence = relevantPolicies.isEmpty()
                ? "Standard Organizational Policy Guidelines"
                : relevantPolicies.stream()
                .map(p -> String.format("Rule %s: %s (Max: %s)", p.getPolicyCode(), p.getTitle(), p.getMaximumAmount()))
                .collect(Collectors.joining("; "));

        String summary = String.format("[Fallback Review] AI Review for %s claim (#%d) of %s %s: Classification = %s.",
                claim.getCategory(), claim.getId() != null ? claim.getId() : 0, claim.getAmount(), claim.getCurrency(), classification);

        return new AiReviewAnalysis(
                classification,
                confidence,
                uncertain,
                summary,
                reasoning.toString().trim(),
                missingInfo.toString().trim(),
                citedEvidence,
                recommendedAction
        );
    }

    public record AiReviewAnalysis(
            String classification,
            Double confidence,
            boolean uncertain,
            String summary,
            String reasoning,
            String missingInformation,
            String policyEvidence,
            String recommendedAction
    ) {}
}
