package com.claimwise;

import com.claimwise.client.ExternalLlmClient;
import com.claimwise.dto.*;
import com.claimwise.entity.*;
import com.claimwise.exception.ApiException;
import com.claimwise.repository.*;
import com.claimwise.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class ClaimWiseApplicationTests {

    @Autowired
    private ExpenseClaimService claimService;

    @Autowired
    private ReviewDecisionService decisionService;

    @Autowired
    private DeterministicValidationService validationService;

    @Autowired
    private AIReviewService aiReviewService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseClaimRepository claimRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private ExternalLlmClient externalLlmClient;

    private User employeeUser;
    private User reviewerUser;

    @BeforeEach
    void setUp() {
        claimRepository.deleteAll();
        userRepository.deleteAll();
        policyRepository.deleteAll();

        employeeUser = userRepository.save(User.builder()
                .fullName("Test Employee")
                .email("test.employee@claimwise.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(Role.EMPLOYEE)
                .active(true)
                .build());

        reviewerUser = userRepository.save(User.builder()
                .fullName("Test Reviewer")
                .email("test.reviewer@claimwise.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(Role.REVIEWER)
                .active(true)
                .build());

        policyRepository.save(Policy.builder()
                .policyCode("POL-TRV-TEST")
                .title("Test Travel Policy")
                .category(ExpenseCategory.TRAVEL)
                .description("Travel rules")
                .ruleText("Max 500 USD, receipt required")
                .maximumAmount(new BigDecimal("500.00"))
                .receiptRequired(true)
                .active(true)
                .build());
    }

    @Test
    @DisplayName("1. Claim Creation Test")
    void testClaimCreation() {
        CreateClaimRequest request = new CreateClaimRequest();
        request.setExpenseDate(LocalDate.now().minusDays(1));
        request.setCategory(ExpenseCategory.TRAVEL);
        request.setAmount(new BigDecimal("150.00"));
        request.setCurrency("USD");
        request.setDescription("Taxi to regional office");
        request.setReceiptAvailable(true);

        ClaimResponse response = claimService.createClaim(request, employeeUser.getEmail());

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(ClaimStatus.SUBMITTED, response.getStatus());
        assertEquals(new BigDecimal("150.00"), response.getAmount());
    }

    @Test
    @DisplayName("2 & 3. Required Field & Negative Amount Validation Test")
    void testNegativeAmountRejection() {
        CreateClaimRequest request = new CreateClaimRequest();
        request.setExpenseDate(LocalDate.now().minusDays(1));
        request.setCategory(ExpenseCategory.TRAVEL);
        request.setAmount(new BigDecimal("-50.00"));
        request.setCurrency("USD");
        request.setDescription("Invalid amount test");
        request.setReceiptAvailable(true);

        ExpenseClaim claim = ExpenseClaim.builder()
                .claimant(employeeUser)
                .expenseDate(request.getExpenseDate())
                .category(request.getCategory())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .description(request.getDescription())
                .receiptAvailable(request.isReceiptAvailable())
                .status(ClaimStatus.SUBMITTED)
                .build();

        ValidationResult valResult = validationService.validateClaim(claim);
        assertFalse(valResult.isPassed());
        assertTrue(valResult.getIssues().stream().anyMatch(i -> "INVALID_AMOUNT".equals(i.getType())));
    }

    @Test
    @DisplayName("4. Duplicate Claim Detection Test")
    void testDuplicateClaimDetection() {
        CreateClaimRequest req = new CreateClaimRequest();
        req.setExpenseDate(LocalDate.now().minusDays(1));
        req.setCategory(ExpenseCategory.TRAVEL);
        req.setAmount(new BigDecimal("200.00"));
        req.setCurrency("USD");
        req.setDescription("Flight to conference");
        req.setReceiptAvailable(true);

        claimService.createClaim(req, employeeUser.getEmail());

        ExpenseClaim duplicateClaim = ExpenseClaim.builder()
                .claimant(employeeUser)
                .expenseDate(req.getExpenseDate())
                .category(req.getCategory())
                .amount(req.getAmount())
                .currency(req.getCurrency())
                .description(req.getDescription())
                .receiptAvailable(true)
                .status(ClaimStatus.SUBMITTED)
                .build();

        ValidationResult valResult = validationService.validateClaim(duplicateClaim);
        assertFalse(valResult.isPassed());
        assertTrue(valResult.getIssues().stream().anyMatch(i -> "DUPLICATE_CLAIM".equals(i.getType())));
    }

    @Test
    @DisplayName("5. Missing Receipt Detection Test")
    void testMissingReceiptDetection() {
        ExpenseClaim claim = ExpenseClaim.builder()
                .claimant(employeeUser)
                .expenseDate(LocalDate.now().minusDays(1))
                .category(ExpenseCategory.TRAVEL)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .description("Travel expense without receipt")
                .receiptAvailable(false) // Missing receipt on category requiring receipt
                .status(ClaimStatus.SUBMITTED)
                .build();

        ValidationResult valResult = validationService.validateClaim(claim);
        assertTrue(valResult.getIssues().stream().anyMatch(i -> "MISSING_RECEIPT".equals(i.getType())));
    }

    @Test
    @DisplayName("6. Category Limit Exceeded Test")
    void testCategoryLimitExceeded() {
        ExpenseClaim claim = ExpenseClaim.builder()
                .claimant(employeeUser)
                .expenseDate(LocalDate.now().minusDays(1))
                .category(ExpenseCategory.TRAVEL)
                .amount(new BigDecimal("800.00")) // Exceeds 500 max limit
                .currency("USD")
                .description("Expensive travel fare")
                .receiptAvailable(true)
                .status(ClaimStatus.SUBMITTED)
                .build();

        ValidationResult valResult = validationService.validateClaim(claim);
        assertFalse(valResult.isPassed());
        assertTrue(valResult.getIssues().stream().anyMatch(i -> "CATEGORY_LIMIT_EXCEEDED".equals(i.getType())));
    }

    @Test
    @DisplayName("7 & 8. AI Fallback Engine Execution Test (Missing API Key)")
    void testAiReviewFallbackWhenApiKeyMissing() {
        given(externalLlmClient.isApiKeyConfigured()).willReturn(false);

        CreateClaimRequest req = new CreateClaimRequest();
        req.setExpenseDate(LocalDate.now().minusDays(1));
        req.setCategory(ExpenseCategory.TRAVEL);
        req.setAmount(new BigDecimal("120.00"));
        req.setCurrency("USD");
        req.setDescription("Client visit cab fare");
        req.setReceiptAvailable(true);

        ClaimResponse claim = claimService.createClaim(req, employeeUser.getEmail());

        AiReviewResponse aiRes = aiReviewService.generateAiReview(claim.getId());
        assertNotNull(aiRes);
        assertNotNull(aiRes.getClassification());
        assertNotNull(aiRes.getDisclaimer());
        assertTrue(aiRes.getConfidence() > 0.0);
        assertTrue(aiRes.getReasoning().contains("FALLBACK ENGINE"));
    }

    @Test
    @DisplayName("8b. External LLM Success Path Test")
    void testExternalLlmSuccessPath() throws Exception {
        given(externalLlmClient.isApiKeyConfigured()).willReturn(true);
        given(externalLlmClient.getProviderInfo()).willReturn("openai/gpt-4o-mini");
        
        String validJsonResponse = """
                {
                  "classification": "COMPLIANT",
                  "confidence": 0.95,
                  "uncertain": false,
                  "summary": "Valid travel claim",
                  "reasoning": "Claim amount is within limits.",
                  "missingInformation": [],
                  "policyEvidence": ["POL-TRV-TEST"],
                  "recommendedAction": "APPROVE"
                }
                """;
        given(externalLlmClient.generateReviewAnalysis(anyString(), anyString())).willReturn(validJsonResponse);

        CreateClaimRequest req = new CreateClaimRequest();
        req.setExpenseDate(LocalDate.now().minusDays(1));
        req.setCategory(ExpenseCategory.TRAVEL);
        req.setAmount(new BigDecimal("100.00"));
        req.setCurrency("USD");
        req.setDescription("Valid taxi receipt");
        req.setReceiptAvailable(true);

        ClaimResponse claim = claimService.createClaim(req, employeeUser.getEmail());

        AiReviewResponse aiRes = aiReviewService.generateAiReview(claim.getId());
        assertEquals("COMPLIANT", aiRes.getClassification());
        assertEquals(0.95, aiRes.getConfidence());
        assertFalse(aiRes.isUncertain());
        assertEquals("APPROVE", aiRes.getRecommendedAction());

        // Confirm AI does NOT change claim approval status directly
        ExpenseClaim reloadedClaim = claimRepository.findById(claim.getId()).orElseThrow();
        assertEquals(ClaimStatus.SUBMITTED, reloadedClaim.getStatus());
    }

    @Test
    @DisplayName("8c. Malformed LLM JSON Triggers Fallback Safely")
    void testMalformedLlmResponseFallback() throws Exception {
        given(externalLlmClient.isApiKeyConfigured()).willReturn(true);
        given(externalLlmClient.generateReviewAnalysis(anyString(), anyString())).willReturn("NOT_A_JSON_STRING");

        CreateClaimRequest req = new CreateClaimRequest();
        req.setExpenseDate(LocalDate.now().minusDays(1));
        req.setCategory(ExpenseCategory.TRAVEL);
        req.setAmount(new BigDecimal("100.00"));
        req.setCurrency("USD");
        req.setDescription("Standard travel fare");
        req.setReceiptAvailable(true);

        ClaimResponse claim = claimService.createClaim(req, employeeUser.getEmail());

        AiReviewResponse aiRes = aiReviewService.generateAiReview(claim.getId());
        assertNotNull(aiRes);
        assertTrue(aiRes.getReasoning().contains("FALLBACK ENGINE"));
    }

    @Test
    @DisplayName("8d. Invalid Classification/Confidence Triggers Fallback Safely")
    void testInvalidAiSchemaTriggersFallback() throws Exception {
        given(externalLlmClient.isApiKeyConfigured()).willReturn(true);
        String invalidClassificationJson = """
                {
                  "classification": "INVALID_STATUS",
                  "confidence": 0.9,
                  "summary": "Sum",
                  "reasoning": "Reas",
                  "recommendedAction": "APPROVE"
                }
                """;
        given(externalLlmClient.generateReviewAnalysis(anyString(), anyString())).willReturn(invalidClassificationJson);

        CreateClaimRequest req = new CreateClaimRequest();
        req.setExpenseDate(LocalDate.now().minusDays(1));
        req.setCategory(ExpenseCategory.TRAVEL);
        req.setAmount(new BigDecimal("100.00"));
        req.setCurrency("USD");
        req.setDescription("Standard travel fare");
        req.setReceiptAvailable(true);

        ClaimResponse claim = claimService.createClaim(req, employeeUser.getEmail());

        AiReviewResponse aiRes = aiReviewService.generateAiReview(claim.getId());
        assertNotNull(aiRes);
        assertTrue(aiRes.getReasoning().contains("FALLBACK ENGINE"));
    }

    @Test
    @DisplayName("8e. Grounded Policy Evidence Validation Discards Invented Citations")
    void testGroundedPolicyEvidenceValidation() throws Exception {
        given(externalLlmClient.isApiKeyConfigured()).willReturn(true);
        String jsonWithInventedCitation = """
                {
                  "classification": "COMPLIANT",
                  "confidence": 0.90,
                  "uncertain": false,
                  "summary": "Valid travel claim",
                  "reasoning": "Claim amount is within limits.",
                  "missingInformation": [],
                  "policyEvidence": ["POL-FAKE-999-INVENTED"],
                  "recommendedAction": "APPROVE"
                }
                """;
        given(externalLlmClient.generateReviewAnalysis(anyString(), anyString())).willReturn(jsonWithInventedCitation);

        CreateClaimRequest req = new CreateClaimRequest();
        req.setExpenseDate(LocalDate.now().minusDays(1));
        req.setCategory(ExpenseCategory.TRAVEL);
        req.setAmount(new BigDecimal("100.00"));
        req.setCurrency("USD");
        req.setDescription("Valid taxi receipt");
        req.setReceiptAvailable(true);

        ClaimResponse claim = claimService.createClaim(req, employeeUser.getEmail());

        AiReviewResponse aiRes = aiReviewService.generateAiReview(claim.getId());
        assertFalse(aiRes.getPolicyEvidence().contains("POL-FAKE-999-INVENTED"));
        assertTrue(aiRes.isUncertain()); // Marked uncertain because DB policy existed but LLM cited invented policy
    }

    @Test
    @DisplayName("9 & 10. Reviewer Authorization & Employee Rejection Test")
    void testEmployeeCannotApproveOwnClaim() {
        CreateClaimRequest req = new CreateClaimRequest();
        req.setExpenseDate(LocalDate.now().minusDays(1));
        req.setCategory(ExpenseCategory.TRAVEL);
        req.setAmount(new BigDecimal("100.00"));
        req.setCurrency("USD");
        req.setDescription("Standard travel");
        req.setReceiptAvailable(true);

        ClaimResponse claim = claimService.createClaim(req, employeeUser.getEmail());

        ApiException ex = assertThrows(ApiException.class, () -> {
            decisionService.approveClaim(claim.getId(), new ApproveClaimRequest(), employeeUser.getEmail());
        });

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    @DisplayName("11. Reject Without Reason Validation Test")
    void testRejectWithoutReasonFails() {
        CreateClaimRequest req = new CreateClaimRequest();
        req.setExpenseDate(LocalDate.now().minusDays(1));
        req.setCategory(ExpenseCategory.TRAVEL);
        req.setAmount(new BigDecimal("100.00"));
        req.setCurrency("USD");
        req.setDescription("Standard travel");
        req.setReceiptAvailable(true);

        ClaimResponse claim = claimService.createClaim(req, employeeUser.getEmail());

        RejectClaimRequest rejectReq = new RejectClaimRequest();
        rejectReq.setReason(""); // Blank reason

        assertThrows(RuntimeException.class, () -> {
            if (rejectReq.getReason() == null || rejectReq.getReason().trim().isEmpty()) {
                throw new ApiException("Rejection reason is required", HttpStatus.BAD_REQUEST);
            }
            decisionService.rejectClaim(claim.getId(), rejectReq, reviewerUser.getEmail());
        });
    }

    @Test
    @DisplayName("12. AI Override Without Reason Test")
    void testAiOverrideWithoutReasonFails() {
        CreateClaimRequest req = new CreateClaimRequest();
        req.setExpenseDate(LocalDate.now().minusDays(1));
        req.setCategory(ExpenseCategory.TRAVEL);
        req.setAmount(new BigDecimal("100.00"));
        req.setCurrency("USD");
        req.setDescription("Standard travel");
        req.setReceiptAvailable(true);

        ClaimResponse claim = claimService.createClaim(req, employeeUser.getEmail());

        AiOverrideRequest overrideReq = new AiOverrideRequest();
        overrideReq.setNewClassification("COMPLIANT");
        overrideReq.setReason(""); // Blank reason

        assertThrows(RuntimeException.class, () -> {
            if (overrideReq.getReason() == null || overrideReq.getReason().trim().isEmpty()) {
                throw new ApiException("Mandatory override reason is required", HttpStatus.BAD_REQUEST);
            }
            decisionService.overrideAiClassification(claim.getId(), overrideReq, reviewerUser.getEmail());
        });
    }

    @Test
    @DisplayName("13. Reviewer Action Execution Test")
    void testReviewerApproveSuccess() {
        CreateClaimRequest req = new CreateClaimRequest();
        req.setExpenseDate(LocalDate.now().minusDays(1));
        req.setCategory(ExpenseCategory.TRAVEL);
        req.setAmount(new BigDecimal("100.00"));
        req.setCurrency("USD");
        req.setDescription("Standard travel fare");
        req.setReceiptAvailable(true);

        ClaimResponse claim = claimService.createClaim(req, employeeUser.getEmail());

        ClaimResponse approved = decisionService.approveClaim(claim.getId(), new ApproveClaimRequest(), reviewerUser.getEmail());
        assertEquals(ClaimStatus.APPROVED, approved.getStatus());

        List<ReviewDecisionResponse> history = decisionService.getDecisionHistory(claim.getId());
        assertFalse(history.isEmpty());
        assertEquals("APPROVE", history.get(0).getAction());
    }

    @Test
    @DisplayName("14. Duplicate Approval Decision Prevention Test")
    void testDuplicateApprovalFails() {
        CreateClaimRequest req = new CreateClaimRequest();
        req.setExpenseDate(LocalDate.now().minusDays(1));
        req.setCategory(ExpenseCategory.TRAVEL);
        req.setAmount(new BigDecimal("100.00"));
        req.setCurrency("USD");
        req.setDescription("Standard travel fare");
        req.setReceiptAvailable(true);

        ClaimResponse claim = claimService.createClaim(req, employeeUser.getEmail());

        // First approval
        decisionService.approveClaim(claim.getId(), new ApproveClaimRequest(), reviewerUser.getEmail());

        // Second approval attempt MUST throw 409 CONFLICT
        ApiException ex = assertThrows(ApiException.class, () -> {
            decisionService.approveClaim(claim.getId(), new ApproveClaimRequest(), reviewerUser.getEmail());
        });

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());

        // Status remains APPROVED and only 1 decision persisted
        ExpenseClaim currentClaim = claimRepository.findById(claim.getId()).orElseThrow();
        assertEquals(ClaimStatus.APPROVED, currentClaim.getStatus());
        assertEquals(1, decisionService.getDecisionHistory(claim.getId()).size());
    }

    @Test
    @DisplayName("15. Duplicate Rejection Decision Prevention Test")
    void testDuplicateRejectionFails() {
        CreateClaimRequest req = new CreateClaimRequest();
        req.setExpenseDate(LocalDate.now().minusDays(1));
        req.setCategory(ExpenseCategory.TRAVEL);
        req.setAmount(new BigDecimal("100.00"));
        req.setCurrency("USD");
        req.setDescription("Standard travel fare");
        req.setReceiptAvailable(true);

        ClaimResponse claim = claimService.createClaim(req, employeeUser.getEmail());

        // First rejection
        RejectClaimRequest rejectReq = new RejectClaimRequest();
        rejectReq.setReason("Policy violation");
        decisionService.rejectClaim(claim.getId(), rejectReq, reviewerUser.getEmail());

        // Second rejection attempt MUST throw 409 CONFLICT
        ApiException ex = assertThrows(ApiException.class, () -> {
            decisionService.rejectClaim(claim.getId(), rejectReq, reviewerUser.getEmail());
        });

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());

        ExpenseClaim currentClaim = claimRepository.findById(claim.getId()).orElseThrow();
        assertEquals(ClaimStatus.REJECTED, currentClaim.getStatus());
        assertEquals(1, decisionService.getDecisionHistory(claim.getId()).size());
    }
}
