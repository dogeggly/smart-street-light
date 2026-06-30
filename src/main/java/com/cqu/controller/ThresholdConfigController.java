package com.cqu.controller;

import com.cqu.service.IThresholdConfigService;
import com.cqu.utils.Result;
import com.cqu.vo.ThresholdConfigVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/threshold-config")
public class ThresholdConfigController {

    @Autowired
    private IThresholdConfigService thresholdConfigService;

    /**
     * 获取阈值配置
     */
    @GetMapping
    public Result<ThresholdConfigVO> getConfig() {
        log.info("获取阈值配置");
        ThresholdConfigVO config = thresholdConfigService.getConfig();
        return Result.success(config);
    }

    /**
     * 更新阈值配置
     */
    @PutMapping
    public Result<String> updateConfig(@RequestBody Map<String, Object> body) {
        BigDecimal lightThresholdOn = body.get("lightThresholdOn") != null
                ? new BigDecimal(body.get("lightThresholdOn").toString()) : null;
        BigDecimal lightThresholdOff = body.get("lightThresholdOff") != null
                ? new BigDecimal(body.get("lightThresholdOff").toString()) : null;
        Integer heartbeatTimeout = body.get("heartbeatTimeout") != null
                ? Integer.valueOf(body.get("heartbeatTimeout").toString()) : null;

        log.info("更新阈值配置: lightThresholdOn={}, lightThresholdOff={}, heartbeatTimeout={}",
                lightThresholdOn, lightThresholdOff, heartbeatTimeout);
        thresholdConfigService.updateConfig(lightThresholdOn, lightThresholdOff, heartbeatTimeout);
        return Result.success("更新成功");
    }
}
