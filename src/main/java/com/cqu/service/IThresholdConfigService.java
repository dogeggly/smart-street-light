package com.cqu.service;

import com.cqu.entity.ThresholdConfig;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cqu.vo.ThresholdConfigVO;

import java.math.BigDecimal;

/**
 * <p>
 * 系统阈值配置表 服务类
 * </p>
 *
 * @author
 * @since 2026-06-29
 */
public interface IThresholdConfigService extends IService<ThresholdConfig> {

    /**
     * 获取当前阈值配置
     */
    ThresholdConfigVO getConfig();

    /**
     * 更新阈值配置
     */
    void updateConfig(BigDecimal lightThresholdOn, BigDecimal lightThresholdOff, Integer heartbeatTimeout);
}
