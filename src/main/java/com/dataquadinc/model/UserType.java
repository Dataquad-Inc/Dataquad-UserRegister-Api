package com.dataquadinc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum UserType {

    ADMIN,
    SUPERADMIN,
    HRMS,
    EMPLOYEE,
    TEAMLEAD,
    BDM,
    PARTNER,
    INVOICE,
    COORDINATOR,
    EXTERNALEMPLOYEE,
    ACCOUNTS,
    RECRUITER,
    SALESEXECUTIVE,
    GRANDSALES;

    @JsonCreator
    public static UserType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Role must not be blank");
        }

        String normalizedValue = value.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(userType -> userType.name().equals(normalizedValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid role: " + value));
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
