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
}

