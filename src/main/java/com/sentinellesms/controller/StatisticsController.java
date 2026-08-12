package com.sentinellesms.controller;

import com.sentinellesms.dto.statistics.StatisticsResponse;
import com.sentinellesms.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    public StatisticsResponse overview() {
        return statisticsService.buildOverview();
    }
}
