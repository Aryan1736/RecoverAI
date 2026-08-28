package com.recoverai.backend.repository.specification;

import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RecoveryCaseSpecifications {

    private RecoveryCaseSpecifications() {
    }

    public static Specification<RecoveryCase> withFilters(UUID merchantId,
                                                          RecoveryCaseStatus status,
                                                          RecoveryPriority priority,
                                                          String failureReasonCategory) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Strictly enforce merchant tenancy
            predicates.add(cb.equal(root.get("merchant").get("id"), merchantId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            if (failureReasonCategory != null && !failureReasonCategory.isBlank()) {
                predicates.add(cb.equal(
                        cb.lower(root.get("failureReasonCategory")),
                        failureReasonCategory.trim().toLowerCase()
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
