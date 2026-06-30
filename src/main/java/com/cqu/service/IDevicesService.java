package com.cqu.service;

import com.cqu.entity.Devices;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cqu.vo.*;

/**
 * <p>
 * 路灯设备表 服务类
 * </p>
 *
 * @author
 * @since 2026-06-29
 */
public interface IDevicesService extends IService<Devices> {

    /**
     * 设备分页列表（支持按名称/开关状态/在线状态筛选）
     */
    PageResult<DeviceVO> pageDevices(int page, int pageSize, String deviceName, String status, String onlineStatus);

    /**
     * 设备详情（含最新光照值和活跃告警数）
     */
    DeviceDetailVO getDeviceDetail(Long id);

    /**
     * 添加设备
     */
    void addDevice(String deviceName, String deviceSn);

    /**
     * 编辑设备名称
     */
    void updateDevice(Long id, String deviceName);

    /**
     * 删除设备（同时清理关联的光照记录和告警日志）
     */
    void deleteDevice(Long id);

    /**
     * 设备概览统计
     */
    DeviceStatisticsVO getStatistics();
}
