package com.sentinellesms.dto.user;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateUserRolesRequest {

    @NotEmpty
    private Set<String> roles;
}
