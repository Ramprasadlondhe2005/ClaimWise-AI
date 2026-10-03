package com.claimwise.service;

import com.claimwise.dto.PolicyResponse;
import com.claimwise.entity.ExpenseCategory;
import com.claimwise.entity.Policy;
import com.claimwise.exception.ResourceNotFoundException;
import com.claimwise.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;

    public List<PolicyResponse> getAllActivePolicies() {
        return policyRepository.findByActiveTrue()
                .stream()
                .map(PolicyResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<PolicyResponse> getPoliciesByCategory(ExpenseCategory category) {
        return policyRepository.findByCategoryAndActiveTrue(category)
                .stream()
                .map(PolicyResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public PolicyResponse getPolicyByCode(String policyCode) {
        Policy policy = policyRepository.findByPolicyCode(policyCode)
                .orElseThrow(() -> new ResourceNotFoundException("Policy Rule", "policyCode", policyCode));
        return PolicyResponse.fromEntity(policy);
    }
}
