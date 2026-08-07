package com.dmsBackend.service.Impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Year;

@Component
public class EmployeeIdGenerator {

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${spring.datasource.driver-class-name}")
    private String databaseDriver;

    @Transactional
    public String generateEmployeeId() {
        int currentYear = Year.now().getValue();
        Long sequenceValue;

        if (databaseDriver.contains("postgresql")) {
            sequenceValue = ((Number) entityManager.createNativeQuery("SELECT nextval('employee_id_seq')").getSingleResult()).longValue();
        } else {
            Object result = entityManager.createNativeQuery("SELECT COALESCE(MAX(CAST(SUBSTRING(employee_id, 8) AS UNSIGNED)), 0) + 1 FROM employee").getSingleResult();
            sequenceValue = ((Number) result).longValue();
        }

        String formattedSequence = String.format("%04d", sequenceValue);
        return currentYear + "AGT" + formattedSequence;
    }
}
