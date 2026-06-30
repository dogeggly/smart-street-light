# 开发日志

所有接口改动记录，按时间倒序排列。

---

## 2026-06-29

### 1. 用户注册

| 项目 | 内容 |
|------|------|
| **请求方式** | `POST` |
| **请求路径** | `/users/register` |
| **请求参数** | `{"username": "string", "password": "string", "role?": "string"}` |
| **返回数据** | `{"code": 200, "errorMsg": null, "data": "注册成功"}` |
| **改动文件** | `UsersController.java`, `IUsersService.java`, `UsersServiceImpl.java` |
| **说明** | 新用户注册，密码使用 BCrypt 加密存储。默认角色为 `MUNICIPAL_STAFF`（市政人员），可传入 `ADMIN` 指定为管理员。用户名重复时返回 500 "用户名已存在"。 |

### 2. 用户登录

| 项目 | 内容 |
|------|------|
| **请求方式** | `POST` |
| **请求路径** | `/users/login` |
| **请求参数** | `{"username": "string", "password": "string"}` |
| **返回数据** | `{"code": 200, "errorMsg": null, "data": {"token": "string", "userId": long, "username": "string", "role": "string"}}` |
| **改动文件** | `UsersController.java`, `IUsersService.java`, `UsersServiceImpl.java` |
| **说明** | 验证用户名密码，成功后返回 JWT token（15 分钟有效）。后续请求在 Header 中携带 `token` 字段。用户名或密码错误时返回 500 "用户名或密码错误"。返回数据使用 `LoginVO` 封装。 |

### 3. 抽取 LoginVO

| 项目 | 内容 |
|------|------|
| **改动文件** | `vo/LoginVO.java`（新增）, `UsersController.java` |
| **说明** | 将登录接口返回的 `Map<String, Object>` 替换为类型安全的 `LoginVO`（token, userId, username, role），使用 Lombok Builder 模式构造。新增 `com.cqu.vo` 包，后续新增 VO 统一放此包下。 |

---

## 2026-06-30

### 基础设施

#### 4. 修复 DDL 缺陷

| 项目 | 内容 |
|------|------|
| **改动文件** | `sql/schema.sql` |
| **说明** | (1) 修正 `control_logs` 建表语句的表名（原来误写成 `light_readings`）；(2) `control_logs.operator_id` 改为允许 NULL（AUTO 自动指令时无操作人）。 |

#### 5. MyBatis-Plus 分页插件

| 项目 | 内容 |
|------|------|
| **改动文件** | `config/MyBatisPlusConfig.java`（新增） |
| **说明** | 注册 `PaginationInnerInterceptor`（PostgreSQL 方言），启用分页查询能力。 |

#### 6. 新增 VO 类（10 个）

| 项目 | 内容 |
|------|------|
| **改动文件** | `vo/` 包下新增 10 个 VO 类 |
| **说明** | `PageResult<T>` — 通用分页结果（total + records）；`DeviceVO`、`DeviceDetailVO`、`DeviceStatisticsVO` — 设备管理；`LightReadingsVO`、`LatestLightVO`、`TrendPointVO` — 光照监测；`AlarmLogVO`、`AlarmStatisticsVO` — 告警管理；`ThresholdConfigVO` — 阈值配置。 |

---

### 设备管理 — `/devices`

#### 7. 设备分页列表 — `GET /devices`

| 项目 | 内容 |
|------|------|
| **请求参数** | `page`(必填, 默认1), `pageSize`(必填, 默认10), `deviceName`(可选, 模糊搜索), `status`(可选, ON/OFF), `onlineStatus`(可选, ONLINE/OFFLINE) |
| **返回数据** | `{"code": 200, "data": {"total": long, "records": [DeviceVO]}}` |
| **改动文件** | `DevicesController.java`, `IDevicesService.java`, `DevicesServiceImpl.java` |
| **说明** | 支持按设备名称模糊搜索、按开关状态和在线状态精确筛选。按创建时间倒序排列。 |

#### 8. 设备详情 — `GET /devices/{id}`

| 项目 | 内容 |
|------|------|
| **返回数据** | `{"code": 200, "data": DeviceDetailVO}` |
| **改动文件** | `DevicesController.java`, `IDevicesService.java`, `DevicesServiceImpl.java` |
| **说明** | 返回设备完整信息，附带最新光照值（`latestLightIntensity`）和活跃告警数（`activeAlarmCount`）。设备不存在时返回 500。 |

#### 9. 添加设备 — `POST /devices`

| 项目 | 内容 |
|------|------|
| **请求体** | `{"deviceName": "string", "deviceSn": "string"}` |
| **返回数据** | `{"code": 200, "data": "添加成功"}` |
| **改动文件** | `DevicesController.java`, `IDevicesService.java`, `DevicesServiceImpl.java` |
| **说明** | 校验 deviceName 和 deviceSn 不可为空，deviceSn 不可重复。 |

#### 10. 编辑设备 — `PUT /devices/{id}`

| 项目 | 内容 |
|------|------|
| **请求体** | `{"deviceName": "string"}` |
| **返回数据** | `{"code": 200, "data": "修改成功"}` |
| **改动文件** | `DevicesController.java`, `IDevicesService.java`, `DevicesServiceImpl.java` |
| **说明** | 只允许修改设备名称，序列号不可改。 |

#### 11. 删除设备 — `DELETE /devices/{id}`

| 项目 | 内容 |
|------|------|
| **返回数据** | `{"code": 200, "data": "删除成功"}` |
| **改动文件** | `DevicesController.java`, `IDevicesService.java`, `DevicesServiceImpl.java` |
| **说明** | 物理删除设备，同时清理该设备关联的所有光照记录和告警日志。 |

#### 12. 设备概览统计 — `GET /devices/statistics`

| 项目 | 内容 |
|------|------|
| **返回数据** | `{"code": 200, "data": {"totalCount": long, "onlineCount": long, "offlineCount": long, "onCount": long, "offCount": long}}` |
| **改动文件** | `DevicesController.java`, `IDevicesService.java`, `DevicesServiceImpl.java` |
| **说明** | Dashboard 首页设备状态统计，返回总数、在线/离线数、开灯/关灯数。 |

---

### 光照监测 — `/light-readings`

#### 13. 光照记录分页列表 — `GET /light-readings`

| 项目 | 内容 |
|------|------|
| **请求参数** | `page`(必填, 默认1), `pageSize`(必填, 默认10), `deviceId`(可选), `startTime`(可选, yyyy-MM-dd HH:mm:ss), `endTime`(可选) |
| **返回数据** | `{"code": 200, "data": {"total": long, "records": [LightReadingsVO]}}` |
| **改动文件** | `LightReadingsController.java`, `ILightReadingsService.java`, `LightReadingsServiceImpl.java` |
| **说明** | 按采集时间倒序排列。`LightReadingsVO` 中携带 `deviceName` 避免前端二次查询。 |

#### 14. 设备最新光照 — `GET /light-readings/latest/{deviceId}`

| 项目 | 内容 |
|------|------|
| **返回数据** | `{"code": 200, "data": {"deviceId": long, "lightIntensity": BigDecimal, "createdAt": "string"}}` |
| **改动文件** | `LightReadingsController.java`, `ILightReadingsService.java`, `LightReadingsServiceImpl.java` |
| **说明** | 获取指定设备的最新一条光照数据。无数据时返回 500。 |

#### 15. 历史光照趋势 — `GET /light-readings/trend`

| 项目 | 内容 |
|------|------|
| **请求参数** | `deviceId`(必填), `startTime`(必填), `endTime`(必填) |
| **返回数据** | `{"code": 200, "data": [{"time": "string", "value": BigDecimal}]}` |
| **改动文件** | `LightReadingsController.java`, `ILightReadingsService.java`, `LightReadingsServiceImpl.java` |
| **说明** | 返回指定时间范围内的光照数据，按时间升序排列，供前端折线图使用。 |

#### 16. 光照数据上报 — `POST /light-readings`

| 项目 | 内容 |
|------|------|
| **请求体** | `{"deviceId": long, "lightIntensity": number}` |
| **返回数据** | `{"code": 200, "data": "上报成功"}` |
| **改动文件** | `LightReadingsController.java`, `ILightReadingsService.java`, `LightReadingsServiceImpl.java` |
| **说明** | 接收硬件/MQTT 网关上报的光照数据。自动开关灯判定逻辑后续实现。 |

---

### 告警管理 — `/alarm-logs`

#### 17. 告警分页列表 — `GET /alarm-logs`

| 项目 | 内容 |
|------|------|
| **请求参数** | `page`(必填, 默认1), `pageSize`(必填, 默认10), `deviceId`(可选), `alarmType`(可选), `status`(可选, ACTIVE/RESOLVED) |
| **返回数据** | `{"code": 200, "data": {"total": long, "records": [AlarmLogVO]}}` |
| **改动文件** | `AlarmLogsController.java`, `IAlarmLogsService.java`, `AlarmLogsServiceImpl.java` |
| **说明** | 按产生时间倒序排列。`AlarmLogVO` 中携带 `deviceName`。 |

#### 18. 告警详情 — `GET /alarm-logs/{id}`

| 项目 | 内容 |
|------|------|
| **返回数据** | `{"code": 200, "data": AlarmLogVO}` |
| **改动文件** | `AlarmLogsController.java`, `IAlarmLogsService.java`, `AlarmLogsServiceImpl.java` |
| **说明** | 查看单条告警完整信息。告警不存在时返回 500。 |

#### 19. 解决告警 — `PUT /alarm-logs/{id}/resolve`

| 项目 | 内容 |
|------|------|
| **返回数据** | `{"code": 200, "data": "处理成功"}` |
| **改动文件** | `AlarmLogsController.java`, `IAlarmLogsService.java`, `AlarmLogsServiceImpl.java` |
| **说明** | status 改为 RESOLVED，resolvedAt 设为当前时间。只有 ACTIVE 状态可被解决，已解决的告警重复操作返回 500。 |

#### 20. 告警统计 — `GET /alarm-logs/statistics`

| 项目 | 内容 |
|------|------|
| **返回数据** | `{"code": 200, "data": {"activeCount": long, "byType": [{"alarmType": "string", "count": long}]}}` |
| **改动文件** | `AlarmLogsController.java`, `IAlarmLogsService.java`, `AlarmLogsServiceImpl.java` |
| **说明** | 活跃告警总数 + 按告警类型分组统计。 |

---

### 阈值配置 — `/threshold-config`

#### 21. 获取阈值 — `GET /threshold-config`

| 项目 | 内容 |
|------|------|
| **返回数据** | `{"code": 200, "data": {"id": 1, "lightThresholdOn": BigDecimal, "lightThresholdOff": BigDecimal, "heartbeatTimeout": int, "updatedAt": "string"}}` |
| **改动文件** | `ThresholdConfigController.java`, `IThresholdConfigService.java`, `ThresholdConfigServiceImpl.java` |
| **说明** | 获取当前系统阈值配置（表中仅一行数据，id=1）。 |

#### 22. 更新阈值 — `PUT /threshold-config`

| 项目 | 内容 |
|------|------|
| **请求体** | `{"lightThresholdOn": number, "lightThresholdOff": number, "heartbeatTimeout": int}` |
| **返回数据** | `{"code": 200, "data": "更新成功"}` |
| **改动文件** | `ThresholdConfigController.java`, `IThresholdConfigService.java`, `ThresholdConfigServiceImpl.java` |
| **说明** | 校验：开灯阈值 < 关灯阈值，心跳超时 > 0。参数为空或校验失败返回 500。 |

