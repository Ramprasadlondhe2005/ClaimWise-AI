package com.claimwise.config;

import com.claimwise.entity.*;
import com.claimwise.repository.ExpenseClaimRepository;
import com.claimwise.repository.PolicyRepository;
import com.claimwise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ExpenseClaimRepository claimRepository;
    private final PolicyRepository policyRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsersAndClaims();
        seedOrganizationalPolicies();
    }

    private void seedUsersAndClaims() {
        if (userRepository.count() == 0) {
            log.info("Seeding development users...");

            User employee = User.builder()
                    .fullName("Alex Morgan (Employee)")
                    .email("employee@claimwise.com")
                    .passwordHash(passwordEncoder.encode("Employee123!"))
                    .role(Role.EMPLOYEE)
                    .active(true)
                    .build();

            User reviewer = User.builder()
                    .fullName("Sarah Jenkins (Reviewer)")
                    .email("reviewer@claimwise.com")
                    .passwordHash(passwordEncoder.encode("Reviewer123!"))
                    .role(Role.REVIEWER)
                    .active(true)
                    .build();

            User admin = User.builder()
                    .fullName("System Admin")
                    .email("admin@claimwise.com")
                    .passwordHash(passwordEncoder.encode("Admin123!"))
                    .role(Role.ADMIN)
                    .active(true)
                    .build();

            userRepository.save(employee);
            userRepository.save(reviewer);
            userRepository.save(admin);

            log.info("Development users seeded successfully:");
            log.info(" -> Employee: employee@claimwise.com / Employee123!");
            log.info(" -> Reviewer: reviewer@claimwise.com / Reviewer123!");
            log.info(" -> Admin:    admin@claimwise.com    / Admin123!");

            // Seed initial sample claims
            ExpenseClaim claim1 = ExpenseClaim.builder()
                    .claimant(employee)
                    .expenseDate(LocalDate.now().minusDays(2))
                    .category(ExpenseCategory.TRAVEL)
                    .amount(new BigDecimal("245.50"))
                    .currency("INR")
                    .description("Taxi and airport transit for Q4 regional sales client meeting")
                    .receiptAvailable(true)
                    .status(ClaimStatus.SUBMITTED)
                    .build();

            ExpenseClaim claim2 = ExpenseClaim.builder()
                    .claimant(employee)
                    .expenseDate(LocalDate.now().minusDays(5))
                    .category(ExpenseCategory.MEALS)
                    .amount(new BigDecimal("85.00")) // Exceeds limit for policy testing!
                    .currency("INR")
                    .description("Dinner with prospective client team")
                    .receiptAvailable(false) // Missing receipt for policy testing!
                    .status(ClaimStatus.SUBMITTED)
                    .build();

            claimRepository.save(claim1);
            claimRepository.save(claim2);
            log.info("Sample expense claims seeded successfully.");
        }
    }

    private void seedOrganizationalPolicies() {
        if (policyRepository.count() == 0) {
            log.info("Seeding organizational expense policy rules...");

            Policy travelPolicy = Policy.builder()
                    .policyCode("POL-TRV-001")
                    .title("Corporate Business Travel & Transit Policy")
                    .category(ExpenseCategory.TRAVEL)
                    .description("Covers flights, rail, rideshare, and taxi transportation incurred during official business travel.")
                    .ruleText("1. Maximum reimbursement per travel claim is $500.00 USD. 2. Itemized receipts are mandatory for all transit charges. 3. First-class fares are strictly non-reimbursable.")
                    .maximumAmount(new BigDecimal("500.00"))
                    .receiptRequired(true)
                    .active(true)
                    .build();

            Policy mealsPolicy = Policy.builder()
                    .policyCode("POL-MEL-001")
                    .title("Business Meals & Client Entertainment Policy")
                    .category(ExpenseCategory.MEALS)
                    .description("Guidelines for individual meals during business travel and authorized client entertainment.")
                    .ruleText("1. Daily meal limit per person is $75.00 USD. 2. Itemized receipts detailing food and beverage purchases are required. 3. Alcohol expenses require prior vice-president approval.")
                    .maximumAmount(new BigDecimal("75.00"))
                    .receiptRequired(true)
                    .active(true)
                    .build();

            Policy hotelPolicy = Policy.builder()
                    .policyCode("POL-ACC-001")
                    .title("Lodging & Accommodation Reimbursement Standard")
                    .category(ExpenseCategory.ACCOMMODATION)
                    .description("Reimbursement criteria for overnight hotel stays during company-sanctioned travel.")
                    .ruleText("1. Standard lodging threshold is $300.00 USD per night. 2. Folio receipts detailing room rate and taxes are required. 3. Personal minibar charges are excluded.")
                    .maximumAmount(new BigDecimal("300.00"))
                    .receiptRequired(true)
                    .active(true)
                    .build();

            Policy officePolicy = Policy.builder()
                    .policyCode("POL-OFF-001")
                    .title("Office Supplies & Equipment Purchasing Policy")
                    .category(ExpenseCategory.OFFICE_SUPPLIES)
                    .description("Covers stationery, computer peripherals, and home-office consumables.")
                    .ruleText("1. Per-order limit is $150.00 USD. 2. Receipts required for purchases over $50.00. 3. IT hardware requires procurement team pre-clearance.")
                    .maximumAmount(new BigDecimal("150.00"))
                    .receiptRequired(false)
                    .active(true)
                    .build();

            Policy commsPolicy = Policy.builder()
                    .policyCode("POL-COM-001")
                    .title("Mobile & High-Speed Data Stipend Policy")
                    .category(ExpenseCategory.COMMUNICATION)
                    .description("Monthly allowance for remote work internet and cellular communication.")
                    .ruleText("1. Monthly maximum is $100.00 USD. 2. Statement copy required if claiming above $50.00 USD.")
                    .maximumAmount(new BigDecimal("100.00"))
                    .receiptRequired(false)
                    .active(true)
                    .build();

            Policy trainingPolicy = Policy.builder()
                    .policyCode("POL-TRN-001")
                    .title("Professional Training & Certification Policy")
                    .category(ExpenseCategory.TRAINING)
                    .description("Reimbursement for approved external courses, technical certifications, and engineering conferences.")
                    .ruleText("1. Per-course maximum is $1000.00 USD. 2. Official completion certificate and payment receipt mandatory. 3. Must be pre-approved by direct manager.")
                    .maximumAmount(new BigDecimal("1000.00"))
                    .receiptRequired(true)
                    .active(true)
                    .build();

            policyRepository.save(travelPolicy);
            policyRepository.save(mealsPolicy);
            policyRepository.save(hotelPolicy);
            policyRepository.save(officePolicy);
            policyRepository.save(commsPolicy);
            policyRepository.save(trainingPolicy);

            log.info("Organizational expense policy rules seeded successfully.");
        }
    }
}
