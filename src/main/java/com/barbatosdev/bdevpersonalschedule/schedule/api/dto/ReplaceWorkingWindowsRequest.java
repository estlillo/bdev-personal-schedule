package com.barbatosdev.bdevpersonalschedule.schedule.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReplaceWorkingWindowsRequest(
    @NotNull List<@Valid WorkingWindowRequest> windows
) {
}

