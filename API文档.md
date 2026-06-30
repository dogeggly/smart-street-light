# 智慧路灯管理平台 — API 接口文档

## 通用约定

| 项目 | 说明 |
|------|------|
| **基础路径** | `http://localhost:8080` |
| **认证方式** | 除注册/登录外，所有请求 Header 需携带 `token`（JWT，15分钟有效） |
| **统一响应格式** | `{"code": 200, "errorMsg": null, "data": ...}` ，成功 code=200，失败 code=500 |
| **分页参数** | `page`（从1开始，默认1）、`pageSize`（默认10），返回 `{"total": long, "records": [...]}` |
| **时间格式** | `yyyy-MM-dd HH:mm:ss`（如 `2026-06-30 10:30:00`） |

---

## 1. 用户模块 — `/users`

### 1.1 用户注册

| 项目 | 内容 |
|------|------|
| **URL** | `POST /users/register` |
| **认证** | 不需要 |
| **请求体** | `{"username": "string", "password": "string", "role?": "string"}` |
| **成功返回** | `{"code": 200, "errorMsg": null, "data": "注册成功"}` |

| 请求字段 | 类型 | 必填 | 说明 |
|----------|------|------|------|
| username | string | 是 | 用户名，不可重复 |
| password | string | 是 | 密码，BCrypt 加密存储 |
| role | string | 否 | 角色：`MUNICIPAL_STAFF`（市政人员，默认）/ `ADMIN`（管理员） |

**作用**：新用户注册，密码 BCrypt 加密，默认角色为市政人员。

---

### 1.2 用户登录

| 项目 | 内容 |
|------|------|
| **URL** | `POST /users/login` |
| **认证** | 不需要 |
| **请求体** | `{"username": "string", "password": "string"}` |
| **成功返回** | `{"code": 200, "errorMsg": null, "data": LoginVO}` |

| 请求字段 | 类型 | 必填 | 说明 |
|----------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |

| 返回字段 (LoginVO) | 类型 | 说明 |
|---------------------|------|------|
| token | string | JWT 令牌，15分钟有效 |
| userId | long | 用户ID |
| username | string | 用户名 |
| role | string | 角色 |

**作用**：验证用户名密码，返回 JWT token，后续请求在 Header 中携带 `token`。

---

## 2. 设备管理 — `/devices`

### 2.1 设备分页列表

| 项目 | 内容 |
|------|------|
| **URL** | `GET /devices` |
| **成功返回** | `{"code": 200, "data": {"total": long, "records": [DeviceVO]}}` |

| 请求参数 | 类型 | 必填 | 默认值 | 说明 |
|----------|------|------|--------|------|
| page | int | 是 | 1 | 页码 |
| pageSize | int | 是 | 10 | 每页条数 |
| deviceName | string | 否 | — | 设备名称（模糊搜索） |
| status | string | 否 | — | 开关状态：`ON` / `OFF` |
| onlineStatus | string | 否 | — | 在线状态：`ONLINE` / `OFFLINE` |

| 返回字段 (DeviceVO) | 类型 | 说明 |
|----------------------|------|------|
| id | long | 设备ID |
| deviceName | string | 设备名称 |
| deviceSn | string | 设备序列号 |
| status | string | 开关状态：ON / OFF |
| onlineStatus | string | 在线状态：ONLINE / OFFLINE |
| lastHeartbeatTime | string | 最近心跳时间 |
| createdAt | string | 创建时间 |

**作用**：分页查询所有路灯设备，支持按名称模糊搜索、按开关/在线状态筛选，按创建时间倒序。

---

### 2.2 设备详情

| 项目 | 内容 |
|------|------|
| **URL** | `GET /devices/{id}` |
| **成功返回** | `{"code": 200, "data": DeviceDetailVO}` |

| 路径参数 | 类型 | 说明 |
|----------|------|------|
| id | long | 设备ID |

| 返回字段 (DeviceDetailVO) | 类型 | 说明 |
|---------------------------|------|------|
| id | long | 设备ID |
| deviceName | string | 设备名称 |
| deviceSn | string | 设备序列号 |
| status | string | 开关状态 |
| onlineStatus | string | 在线状态 |
| lastHeartbeatTime | string | 最近心跳时间 |
| latestLightIntensity | number | 最新光照强度值 |
| activeAlarmCount | long | 活跃告警数 |
| createdAt | string | 创建时间 |

**作用**：查看单个设备完整信息，附带最新光照值和活跃告警数量。

---

### 2.3 添加设备

| 项目 | 内容 |
|------|------|
| **URL** | `POST /devices` |
| **请求体** | `{"deviceName": "string", "deviceSn": "string"}` |
| **成功返回** | `{"code": 200, "data": "添加成功"}` |

| 请求字段 | 类型 | 必填 | 说明 |
|----------|------|------|------|
| deviceName | string | 是 | 设备名称 |
| deviceSn | string | 是 | 设备序列号（唯一，作为MQTT客户端标识） |

**作用**：注册新路灯设备，校验名称和序列号不为空、序列号不可重复。

---

### 2.4 编辑设备

| 项目 | 内容 |
|------|------|
| **URL** | `PUT /devices/{id}` |
| **请求体** | `{"deviceName": "string"}` |
| **成功返回** | `{"code": 200, "data": "修改成功"}` |

| 路径参数 | 类型 | 说明 |
|----------|------|------|
| id | long | 设备ID |

| 请求字段 | 类型 | 必填 | 说明 |
|----------|------|------|------|
| deviceName | string | 是 | 新设备名称 |

**作用**：修改设备名称，序列号不可修改。

---

### 2.5 删除设备

| 项目 | 内容 |
|------|------|
| **URL** | `DELETE /devices/{id}` |
| **成功返回** | `{"code": 200, "data": "删除成功"}` |

| 路径参数 | 类型 | 说明 |
|----------|------|------|
| id | long | 设备ID |

**作用**：物理删除设备，同时清理该设备关联的所有光照记录和告警日志。

---

### 2.6 设备概览统计

| 项目 | 内容 |
|------|------|
| **URL** | `GET /devices/statistics` |
| **成功返回** | `{"code": 200, "data": DeviceStatisticsVO}` |

| 返回字段 (DeviceStatisticsVO) | 类型 | 说明 |
|-------------------------------|------|------|
| totalCount | long | 设备总数 |
| onlineCount | long | 在线设备数 |
| offlineCount | long | 离线设备数 |
| onCount | long | 已开灯设备数 |
| offCount | long | 已关灯设备数 |

**作用**：Dashboard 首页设备状态概览统计。

---

## 3. 光照监测 — `/light-readings`

### 3.1 光照记录分页列表

| 项目 | 内容 |
|------|------|
| **URL** | `GET /light-readings` |
| **成功返回** | `{"code": 200, "data": {"total": long, "records": [LightReadingsVO]}}` |

| 请求参数 | 类型 | 必填 | 默认值 | 说明 |
|----------|------|------|--------|------|
| page | int | 是 | 1 | 页码 |
| pageSize | int | 是 | 10 | 每页条数 |
| deviceId | long | 否 | — | 设备ID |
| startTime | string | 否 | — | 开始时间（yyyy-MM-dd HH:mm:ss） |
| endTime | string | 否 | — | 结束时间（yyyy-MM-dd HH:mm:ss） |

| 返回字段 (LightReadingsVO) | 类型 | 说明 |
|----------------------------|------|------|
| id | long | 记录ID |
| deviceId | long | 设备ID |
| deviceName | string | 设备名称 |
| lightIntensity | number | 光照强度值 |
| createdAt | string | 采集时间 |

**作用**：分页查询光照采集记录，支持按设备、时间范围筛选，按采集时间倒序。

---

### 3.2 设备最新光照

| 项目 | 内容 |
|------|------|
| **URL** | `GET /light-readings/latest/{deviceId}` |
| **成功返回** | `{"code": 200, "data": LatestLightVO}` |

| 路径参数 | 类型 | 说明 |
|----------|------|------|
| deviceId | long | 设备ID |

| 返回字段 (LatestLightVO) | 类型 | 说明 |
|--------------------------|------|------|
| deviceId | long | 设备ID |
| lightIntensity | number | 最新光照强度值 |
| createdAt | string | 采集时间 |

**作用**：获取指定设备最新一条光照数据，用于设备详情卡片展示。

---

### 3.3 历史光照趋势

| 项目 | 内容 |
|------|------|
| **URL** | `GET /light-readings/trend` |
| **成功返回** | `{"code": 200, "data": [TrendPointVO]}` |

| 请求参数 | 类型 | 必填 | 说明 |
|----------|------|------|------|
| deviceId | long | 是 | 设备ID |
| startTime | string | 是 | 开始时间（yyyy-MM-dd HH:mm:ss） |
| endTime | string | 是 | 结束时间（yyyy-MM-dd HH:mm:ss） |

| 返回字段 (TrendPointVO) | 类型 | 说明 |
|-------------------------|------|------|
| time | string | 采集时间 |
| value | number | 光照强度值 |

**作用**：查询指定时间范围内的光照变化，按时间升序排列，供前端折线图使用。

---

### 3.4 光照数据上报

| 项目 | 内容 |
|------|------|
| **URL** | `POST /light-readings` |
| **请求体** | `{"deviceId": long, "lightIntensity": number}` |
| **成功返回** | `{"code": 200, "data": "上报成功"}` |

| 请求字段 | 类型 | 必填 | 说明 |
|----------|------|------|------|
| deviceId | long | 是 | 设备ID |
| lightIntensity | number | 是 | 光照强度值 |

**作用**：接收硬件/MQTT 网关上报的光照数据，写入光照记录表。

---

## 4. 告警管理 — `/alarm-logs`

### 4.1 告警分页列表

| 项目 | 内容 |
|------|------|
| **URL** | `GET /alarm-logs` |
| **成功返回** | `{"code": 200, "data": {"total": long, "records": [AlarmLogVO]}}` |

| 请求参数 | 类型 | 必填 | 默认值 | 说明 |
|----------|------|------|--------|------|
| page | int | 是 | 1 | 页码 |
| pageSize | int | 是 | 10 | 每页条数 |
| deviceId | long | 否 | — | 设备ID |
| alarmType | string | 否 | — | 告警类型（如 `OFFLINE`） |
| status | string | 否 | — | 告警状态：`ACTIVE` / `RESOLVED` |

| 返回字段 (AlarmLogVO) | 类型 | 说明 |
|-----------------------|------|------|
| id | long | 告警ID |
| deviceId | long | 设备ID |
| deviceName | string | 设备名称 |
| alarmType | string | 告警类型 |
| message | string | 告警详情 |
| status | string | 状态：ACTIVE / RESOLVED |
| createdAt | string | 告警产生时间 |
| resolvedAt | string | 解决时间（null 表示未解决） |

**作用**：分页查询告警记录，支持按设备、类型、状态筛选，按产生时间倒序。

---

### 4.2 告警详情

| 项目 | 内容 |
|------|------|
| **URL** | `GET /alarm-logs/{id}` |
| **成功返回** | `{"code": 200, "data": AlarmLogVO}` |

| 路径参数 | 类型 | 说明 |
|----------|------|------|
| id | long | 告警ID |

**作用**：查看单条告警的完整信息（返回结构与 4.1 相同）。

---

### 4.3 解决告警

| 项目 | 内容 |
|------|------|
| **URL** | `PUT /alarm-logs/{id}/resolve` |
| **成功返回** | `{"code": 200, "data": "处理成功"}` |

| 路径参数 | 类型 | 说明 |
|----------|------|------|
| id | long | 告警ID |

**作用**：将告警标记为已解决（ACTIVE → RESOLVED），记录解决时间。已解决的告警不可重复操作。

---

### 4.4 告警统计

| 项目 | 内容 |
|------|------|
| **URL** | `GET /alarm-logs/statistics` |
| **成功返回** | `{"code": 200, "data": AlarmStatisticsVO}` |

| 返回字段 (AlarmStatisticsVO) | 类型 | 说明 |
|------------------------------|------|------|
| activeCount | long | 活跃告警总数 |
| byType | array | 按类型分组统计 |
| byType[].alarmType | string | 告警类型 |
| byType[].count | long | 该类型数量 |

**作用**：Dashboard 告警概览，展示活跃告警总数及按类型分组统计。

---

## 5. 阈值配置 — `/threshold-config`

### 5.1 获取阈值

| 项目 | 内容 |
|------|------|
| **URL** | `GET /threshold-config` |
| **成功返回** | `{"code": 200, "data": ThresholdConfigVO}` |

| 返回字段 (ThresholdConfigVO) | 类型 | 说明 |
|------------------------------|------|------|
| id | long | 配置ID（固定为 1） |
| lightThresholdOn | number | 光照开灯阈值（低于此值自动开灯） |
| lightThresholdOff | number | 光照关灯阈值（高于此值自动关灯） |
| heartbeatTimeout | int | 心跳超时秒数（超时判定离线） |
| updatedAt | string | 最后更新时间 |

**作用**：获取当前系统的阈值配置参数。

---

### 5.2 更新阈值

| 项目 | 内容 |
|------|------|
| **URL** | `PUT /threshold-config` |
| **请求体** | `{"lightThresholdOn": number, "lightThresholdOff": number, "heartbeatTimeout": int}` |
| **成功返回** | `{"code": 200, "data": "更新成功"}` |

| 请求字段 | 类型 | 必填 | 说明 |
|----------|------|------|------|
| lightThresholdOn | number | 是 | 开灯阈值，必须 < 关灯阈值 |
| lightThresholdOff | number | 是 | 关灯阈值，必须 > 开灯阈值 |
| heartbeatTimeout | int | 是 | 心跳超时秒数，必须 > 0 |

**作用**：修改光照自动开关阈值和心跳超时参数。

---

## 附录 A：通用分页返回结构

```json
{
  "code": 200,
  "errorMsg": null,
  "data": {
    "total": 100,
    "records": [ ... ]
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| total | long | 总记录数 |
| records | array | 当前页数据列表 |

## 附录 B：错误返回结构

```json
{
  "code": 500,
  "errorMsg": "错误描述信息",
  "data": null
}
```
