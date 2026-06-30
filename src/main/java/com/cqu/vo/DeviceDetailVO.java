package com.cqu.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备详情（含最新光照值和活跃告警数）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDetailVO {

    private Long id;
    private String deviceName;
    private String deviceSn;
    private String status;
    private String onlineStatus;
    private LocalDateTime lastHeartbeatTime;
    private BigDecimal latestLightIntensity;
    private Long activeAlarmCount;
    private LocalDateTime createdAt;
}
