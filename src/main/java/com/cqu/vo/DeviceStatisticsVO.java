package com.cqu.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备概览统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStatisticsVO {

    private Long totalCount;
    private Long onlineCount;
    private Long offlineCount;
    private Long onCount;
    private Long offCount;
}
