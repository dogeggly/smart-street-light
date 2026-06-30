package com.cqu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 路灯设备表
 * </p>
 *
 * @author 
 * @since 2026-06-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("devices")
public class Devices implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private Long id;

    private String deviceName;

    /**
     * 硬件唯一序列号，MQTT主题标识
     */
    private String deviceSn;

    /**
     * 开关状态: ON-已开灯, OFF-已关灯
     */
    private String status;

    /**
     * 在线状态: ONLINE-在线, OFFLINE-离线
     */
    private String onlineStatus;

    private LocalDateTime lastHeartbeatTime;

    private LocalDateTime createdAt;


}
