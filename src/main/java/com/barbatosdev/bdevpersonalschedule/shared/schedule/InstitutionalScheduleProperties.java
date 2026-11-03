package com.barbatosdev.bdevpersonalschedule.shared.schedule;

import java.time.LocalTime;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "institutional.schedule")
public class InstitutionalScheduleProperties {

    private LocalTime dayStart = LocalTime.parse("08:00");
    private LocalTime dayEnd = LocalTime.parse("17:00");
    private int slotMinutes = 45;

    private LocalTime healthyBreakStart = LocalTime.parse("10:15");
    private LocalTime healthyBreakEnd = LocalTime.parse("10:30");

    private LocalTime lunchBreakStart = LocalTime.parse("13:00");
    private LocalTime lunchBreakEnd = LocalTime.parse("14:00");

    public LocalTime getDayStart() {
        return dayStart;
    }

    public void setDayStart(LocalTime dayStart) {
        this.dayStart = dayStart;
    }

    public LocalTime getDayEnd() {
        return dayEnd;
    }

    public void setDayEnd(LocalTime dayEnd) {
        this.dayEnd = dayEnd;
    }

    public int getSlotMinutes() {
        return slotMinutes;
    }

    public void setSlotMinutes(int slotMinutes) {
        this.slotMinutes = slotMinutes;
    }

    public LocalTime getHealthyBreakStart() {
        return healthyBreakStart;
    }

    public void setHealthyBreakStart(LocalTime healthyBreakStart) {
        this.healthyBreakStart = healthyBreakStart;
    }

    public LocalTime getHealthyBreakEnd() {
        return healthyBreakEnd;
    }

    public void setHealthyBreakEnd(LocalTime healthyBreakEnd) {
        this.healthyBreakEnd = healthyBreakEnd;
    }

    public LocalTime getLunchBreakStart() {
        return lunchBreakStart;
    }

    public void setLunchBreakStart(LocalTime lunchBreakStart) {
        this.lunchBreakStart = lunchBreakStart;
    }

    public LocalTime getLunchBreakEnd() {
        return lunchBreakEnd;
    }

    public void setLunchBreakEnd(LocalTime lunchBreakEnd) {
        this.lunchBreakEnd = lunchBreakEnd;
    }
}

