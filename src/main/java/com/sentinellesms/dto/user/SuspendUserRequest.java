package com.sentinellesms.dto.user;

import lombok.Data;

@Data
public class SuspendUserRequest {

    private boolean enabled;
    private String reason;
}
