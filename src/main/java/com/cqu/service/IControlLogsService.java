package com.cqu.service;

import com.cqu.entity.ControlLogs;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cqu.vo.ControlLogVO;
import com.cqu.vo.PageResult;

/**
 * <p>
 * 路灯控制指令日志 服务类
 * </p>
 *
 * @author
 * @since 2026-06-29
 */
public interface IControlLogsService extends IService<ControlLogs> {

    /**
     * 控制日志分页列表（按设备/操作类型/操作人筛选，按时间倒序）
     */
    PageResult<ControlLogVO> pageLogs(int page, int pageSize, Long deviceId, String command, Long operatorId);

    /**
     * 控制日志详情
     */
    ControlLogVO getDetail(Long id);

    /**
     * 记录操作日志（手动操作，从 UserHolder 获取操作人）
     *
     * @param deviceId   关联设备ID（可为 null）
     * @param command    操作类型
     * @param result     执行结果
     */
    void recordLog(Long deviceId, String command, String result);

    /**
     * 记录操作日志（指定来源，用于自动/硬件侧操作）
     *
     * @param deviceId   关联设备ID（可为 null）
     * @param command    操作类型
     * @param result     执行结果
     * @param source     指令来源（AUTO / MANUAL）
     */
    void recordLog(Long deviceId, String command, String result, String source);
}
