package com.cqu.service.impl;

import com.cqu.entity.ThresholdConfig;
import com.cqu.mapper.ThresholdConfigMapper;
import com.cqu.service.IControlLogsService;
import com.cqu.service.IThresholdConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqu.vo.ThresholdConfigVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 系统阈值配置表 服务实现类
 * </p>
 *
 * @author
 * @since 2026-06-29
 */
@Service
public class ThresholdConfigServiceImpl extends ServiceImpl<ThresholdConfigMapper, ThresholdConfig> implements IThresholdConfigService {

    /** 阈值配置固定ID */
    private static final Long CONFIG_ID = 1L;

    @Autowired
    private IControlLogsService controlLogsService;

    @Override
    public ThresholdConfigVO getConfig() {
        ThresholdConfig config = this.getById(CONFIG_ID);
        if (config == null) {
            throw new RuntimeException("阈值配置不存在，请先初始化数据库");
        }

        return ThresholdConfigVO.builder()
                .id(config.getId())
                .lightThresholdOn(config.getLightThresholdOn())
                .lightThresholdOff(config.getLightThresholdOff())
                .heartbeatTimeout(config.getHeartbeatTimeout())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    @Override
    public void updateConfig(BigDecimal lightThresholdOn, BigDecimal lightThresholdOff, Integer heartbeatTimeout) {
        if (lightThresholdOn == null || lightThresholdOff == null || heartbeatTimeout == null) {
            throw new RuntimeException("阈值参数不能为空");
        }
        if (lightThresholdOn.compareTo(lightThresholdOff) >= 0) {
            throw new RuntimeException("开灯阈值必须小于关灯阈值");
        }
        if (heartbeatTimeout <= 0) {
            throw new RuntimeException("心跳超时时间必须大于0");
        }

        ThresholdConfig config = this.getById(CONFIG_ID);
        if (config == null) {
            throw new RuntimeException("阈值配置不存在");
        }

        config.setLightThresholdOn(lightThresholdOn);
        config.setLightThresholdOff(lightThresholdOff);
        config.setHeartbeatTimeout(heartbeatTimeout);
        config.setUpdatedAt(LocalDateTime.now());
        this.updateById(config);
        controlLogsService.recordLog(null, "UPDATE_THRESHOLD", "SUCCESS");
    }
}
