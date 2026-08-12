package com.sentinellesms.dto.model;

import com.sentinellesms.dto.pattern.FraudPatternResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelSyncResponse {

    private ModelVersionResponse modelVersion;

    private List<FraudPatternResponse> patterns;
}
