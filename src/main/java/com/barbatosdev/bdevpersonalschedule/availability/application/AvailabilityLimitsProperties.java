package com.barbatosdev.bdevpersonalschedule.availability.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "availability")
public class AvailabilityLimitsProperties {

    private int maxRangeDays = 30;
    private int maxProfessionals = 7;
    private int maxReturnedSlots = 500;
    private int defaultStepMinutes = 15;

    public int getMaxRangeDays() {
        return maxRangeDays;
    }

    public void setMaxRangeDays(int maxRangeDays) {
        this.maxRangeDays = maxRangeDays;
    }

    public int getMaxProfessionals() {
        return maxProfessionals;
    }

    public void setMaxProfessionals(int maxProfessionals) {
        this.maxProfessionals = maxProfessionals;
    }

    public int getMaxReturnedSlots() {
        return maxReturnedSlots;
    }

    public void setMaxReturnedSlots(int maxReturnedSlots) {
        this.maxReturnedSlots = maxReturnedSlots;
    }

    public int getDefaultStepMinutes() {
        return defaultStepMinutes;
    }

    public void setDefaultStepMinutes(int defaultStepMinutes) {
        this.defaultStepMinutes = defaultStepMinutes;
    }
}

