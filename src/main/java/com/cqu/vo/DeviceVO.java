package com.cqu.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备列表项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceVO {

    private Long id;
    private String deviceName;
    private String deviceSn;
    private String status;
    private String onlineStatus;
    private LocalDateTime lastHeartbeatTime;
    private LocalDateTime createdAt;
}
