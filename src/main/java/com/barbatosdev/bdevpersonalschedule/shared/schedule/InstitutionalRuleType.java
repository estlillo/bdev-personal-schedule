package com.barbatosdev.bdevpersonalschedule.shared.schedule;

public enum InstitutionalRuleType {
    WEEKLY,
    DATE_RANGE;

    public static InstitutionalRuleType fromValue(String value) {
        for (InstitutionalRuleType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("ruleType must be WEEKLY or DATE_RANGE");
    }
}

