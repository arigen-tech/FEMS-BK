package com.dmsBackend.constants;

import com.dmsBackend.entity.DocumentActivityReport;
import com.dmsBackend.response.DocumentActivityReportRequest;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class DocumentActivityReportSpecification {
    public static Specification<DocumentActivityReport> search(
            DocumentActivityReportRequest request) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // REQUIRED: Action Types (Multiple)
            if (request.getActionTypes() != null && !request.getActionTypes().isEmpty()) {
                predicates.add(root.get("actionType").in(request.getActionTypes()));
            }

            if (request.getBranchId() != null) {
                predicates.add(cb.equal(root.get("branchId"), request.getBranchId()));
            }

            if (request.getDepartmentId() != null) {
                predicates.add(cb.equal(root.get("departmentId"), request.getDepartmentId()));
            }

            if (request.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("categoryId"), request.getCategoryId()));
            }

            if (request.getEmpId() != null) {
                predicates.add(cb.equal(root.get("empId"), request.getEmpId()));
            }

            if (request.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("actionDate"), request.getFromDate()));
            }

            if (request.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("actionDate"), request.getToDate()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
