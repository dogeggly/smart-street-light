package com.cqu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cqu.entity.ControlLogs;
import com.cqu.entity.Devices;
import com.cqu.entity.Users;
import com.cqu.mapper.ControlLogsMapper;
import com.cqu.mapper.DevicesMapper;
import com.cqu.mapper.UsersMapper;
import com.cqu.service.IControlLogsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqu.utils.UserHolder;
import com.cqu.vo.ControlLogVO;
import com.cqu.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 路灯控制指令日志 服务实现类
 * </p>
 *
 * @author
 * @since 2026-06-29
 */
@Service
public class ControlLogsServiceImpl extends ServiceImpl<ControlLogsMapper, ControlLogs> implements IControlLogsService {

    @Autowired
    private DevicesMapper devicesMapper;

    @Autowired
    private UsersMapper usersMapper;

    @Override
    public PageResult<ControlLogVO> pageLogs(int page, int pageSize, Long deviceId, String command, Long operatorId) {
        LambdaQueryWrapper<ControlLogs> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(deviceId != null, ControlLogs::getDeviceId, deviceId);
        wrapper.eq(command != null && !command.isBlank(), ControlLogs::getCommand, command);
        wrapper.eq(operatorId != null, ControlLogs::getOperatorId, operatorId);
        wrapper.orderByDesc(ControlLogs::getCreatedAt);

        Page<ControlLogs> pageResult = this.page(new Page<>(page, pageSize), wrapper);

        // 批量获取设备名称和操作人名称
        Map<Long, String> deviceNameMap = buildDeviceNameMap(pageResult.getRecords());
        Map<Long, String> operatorNameMap = buildOperatorNameMap(pageResult.getRecords());

        List<ControlLogVO> records = pageResult.getRecords().stream()
                .map(log -> toControlLogVO(log, deviceNameMap.get(log.getDeviceId()), operatorNameMap.get(log.getOperatorId())))
                .collect(Collectors.toList());

        return PageResult.of(pageResult.getTotal(), records);
    }

    @Override
    public ControlLogVO getDetail(Long id) {
        ControlLogs log = this.getById(id);
        if (log == null) {
            throw new RuntimeException("控制日志不存在");
        }

        String deviceName = null;
        if (log.getDeviceId() != null) {
            Devices device = devicesMapper.selectById(log.getDeviceId());
            if (device != null) {
                deviceName = device.getDeviceName();
            }
        }

        String operatorName = null;
        if (log.getOperatorId() != null) {
            Users operator = usersMapper.selectById(log.getOperatorId());
            if (operator != null) {
                operatorName = operator.getUsername();
            }
        }

        return toControlLogVO(log, deviceName, operatorName);
    }

    @Override
    public void recordLog(Long deviceId, String command, String result) {
        recordLog(deviceId, command, result, "MANUAL");
    }

    @Override
    public void recordLog(Long deviceId, String command, String result, String source) {
        ControlLogs log = new ControlLogs();
        log.setDeviceId(deviceId);
        log.setOperatorId(UserHolder.getCurrent());
        log.setCommand(command);
        log.setSource(source);
        log.setResult(result);
        this.save(log);
    }

    private Map<Long, String> buildDeviceNameMap(List<ControlLogs> logs) {
        List<Long> deviceIds = logs.stream()
                .map(ControlLogs::getDeviceId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        if (deviceIds.isEmpty()) {
            return Map.of();
        }

        return devicesMapper.selectBatchIds(deviceIds).stream()
                .collect(Collectors.toMap(Devices::getId, Devices::getDeviceName));
    }

    private Map<Long, String> buildOperatorNameMap(List<ControlLogs> logs) {
        List<Long> operatorIds = logs.stream()
                .map(ControlLogs::getOperatorId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        if (operatorIds.isEmpty()) {
            return Map.of();
        }

        return usersMapper.selectBatchIds(operatorIds).stream()
                .collect(Collectors.toMap(Users::getId, Users::getUsername));
    }

    private ControlLogVO toControlLogVO(ControlLogs log, String deviceName, String operatorName) {
        return ControlLogVO.builder()
                .id(String.valueOf(log.getId()))
                .deviceId(String.valueOf(log.getDeviceId()))
                .deviceName(deviceName)
                .operatorId(String.valueOf(log.getOperatorId()))
                .operatorName(operatorName)
                .command(log.getCommand())
                .source(log.getSource())
                .result(log.getResult())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
