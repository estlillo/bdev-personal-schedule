package com.barbatosdev.bdevpersonalschedule.schedule.api.dto;

import java.util.List;

public record WorkingWindowsResponse(
    List<WorkingWindowResponse> windows
) {
}

