# OpenCV 图像识别模块设计思路

## Demo 场景

当前落地目标不是通用图像平台，而是一个厨房帮手小 demo：

- 使用 YOLO 识别食材、配料、厨具
- 将识别结果交给 Agent 分析
- 根据现有食材推荐可制作菜谱
- 给出缺失配料、烹饪步骤、口味建议

## 1. 目标说明

当前已确认两项约束：

- YOLO 采用方案 B：模型转 ONNX，Java 直接推理
- 人脸登录只做纯后端接口，不做前端演示页

当前小 demo 的主业务目标是：

- 用户上传厨房台面或冰箱食材图片
- 系统识别现有食材和配料
- Agent 基于识别结果推荐菜谱
- 输出推荐理由、缺失食材、做法步骤和注意事项

## 2. 总体落位建议

### 2.1 模块职责

- `opencv`
  - 图像预处理、YOLO 推理、后处理、特征提取
- `user`
  - 人脸绑定、人脸登录、Sa-Token 登录态
- `agent`
  - 基于视觉结果的多智能体协作分析与菜谱推荐
- `common`
  - 人脸模板、图像任务、检测结果等公共实体与仓储

## 3. 人脸识别登录

### 3.1 业务链路

账号密码登录后绑定人脸，后续通过人脸图像完成自动登录。

### 3.2 接口建议

- `POST /auth/face/bind`
- `POST /auth/face/login`
- `GET /auth/face/status`
- `DELETE /auth/face/unbind`

### 3.3 核心校验

- 单人脸检测
- 活体检测
- 质量分阈值
- 相似度阈值
- 登录失败次数限制

### 3.4 表结构建议

`sy_user_face_template`

- `user_id`
- `tenant_id`
- `face_template_code`
- `embedding_cipher_text`
- `embedding_dimension`
- `embedding_version`
- `quality_score`
- `liveness_score`
- `source_image_url`
- `image_sha256`
- `status`
- `last_verified_time`

## 4. YOLO + Java ONNX 推理

### 4.0 厨房帮手识别目标

第一期建议只覆盖厨房 demo 所需标签，不要一开始就做过大的通用类别。

建议第一期标签：

- 食材
  - `TOMATO`
  - `POTATO`
  - `ONION`
  - `EGG`
  - `CUCUMBER`
  - `CARROT`
  - `PEPPER`
  - `BROCCOLI`
  - `MUSHROOM`
  - `CHICKEN`
  - `PORK`
  - `BEEF`
  - `SHRIMP`
  - `TOFU`
- 配料
  - `GARLIC`
  - `GINGER`
  - `SCALLION`
  - `CHILI`
- 厨具
  - `PAN`
  - `POT`
  - `KNIFE`
  - `CUTTING_BOARD`

如果训练成本有限，第一期可以先只做食材和基础配料，不识别厨具。

### 4.1 推荐方式

第一期优先用 `ONNX Runtime`，必要时后续再抽象到 `DJL`。

### 4.2 模块划分

- `YoloOnnxInferenceClient`
  - 加载 ONNX 模型并执行推理
- `YoloPostProcessService`
  - NMS、阈值过滤、结果标准化
- `YoloDetectionService`
  - 对外统一图像检测能力

### 4.3 接口建议

- `POST /image/detect`
- `POST /image/agent/analyze`

对于厨房帮手 demo，建议把第二个接口语义明确为：

- `POST /image/recipe/recommend`

### 4.4 输出标准

- `label`
- `classCode`
- `confidence`
- `x`
- `y`
- `width`
- `height`
- `areaRatio`
- `attributes`

厨房场景建议补充两个推断字段：

- `estimatedCount`
- `ingredientCategory`

例如：

- `ingredientCategory=VEGETABLE`
- `ingredientCategory=MEAT`
- `ingredientCategory=SEASONING`

## 5. 多智能体协作

### 5.1 链路

`opencv` 输出结构化检测结果，`agent` 消费结果并完成食材理解、菜谱匹配、缺料分析、步骤生成、结果汇总。

### 5.2 推荐角色

- 食材理解 Agent
  - 负责整理识别到的食材、配料、数量和类别
- 菜谱匹配 Agent
  - 负责根据现有食材匹配适合制作的菜谱
- 缺料分析 Agent
  - 负责判断当前可直接制作哪些菜，以及缺哪些关键配料
- 烹饪建议 Agent
  - 负责生成做法步骤、火候建议、口味提醒
- 汇总 Agent
  - 负责输出最终推荐结果

### 5.3 推荐分析结果

建议返回的推荐结果至少包含：

- `recipeName`
- `score`
- `reason`
- `availableIngredients`
- `missingIngredients`
- `optionalIngredients`
- `steps`
- `tips`
- `difficulty`
- `cookTimeMinutes`

### 5.4 推荐逻辑建议

建议先做“规则 + Agent”的混合模式，而不是完全依赖大模型自由发挥：

1. YOLO 识别出食材列表
2. 后端先做基础归一化
   - 例如 `西红柿` 归一为 `TOMATO`
3. 先用规则库筛一遍可做菜谱候选
4. 再交给 Agent 做排序、解释和步骤补全

这样比完全直接让 Agent 幻想菜单更稳。

### 5.5 推荐菜谱示例

如果识别到：

- `EGG`
- `TOMATO`
- `SCALLION`

则可推荐：

- 西红柿炒鸡蛋
- 鸡蛋番茄汤

如果识别到：

- `POTATO`
- `PEPPER`
- `PORK`

则可推荐：

- 土豆青椒炒肉
- 家常土豆肉片

## 6. 厨房帮手接口建议

### 6.1 食材识别接口

`POST /image/detect`

请求体建议：

```json
{
  "imageBase64": "base64图片",
  "imageFormat": "jpg",
  "businessScene": "KITCHEN_ASSISTANT"
}
```

返回体建议：

```json
{
  "success": true,
  "code": "200",
  "message": "识别成功",
  "data": {
    "detectCount": 3,
    "modelName": "yolo-kitchen-v1",
    "detections": [
      {
        "label": "egg",
        "classCode": "EGG",
        "confidence": 0.96,
        "ingredientCategory": "PROTEIN",
        "estimatedCount": 4
      },
      {
        "label": "tomato",
        "classCode": "TOMATO",
        "confidence": 0.94,
        "ingredientCategory": "VEGETABLE",
        "estimatedCount": 2
      }
    ]
  }
}
```

### 6.2 菜谱推荐接口

`POST /image/recipe/recommend`

请求体建议：

```json
{
  "imageBase64": "base64图片",
  "imageFormat": "jpg",
  "businessScene": "KITCHEN_ASSISTANT",
  "userPrompt": "根据现有食材推荐两道家常菜",
  "preferredTaste": "HOME_STYLE",
  "excludeIngredients": [
    "CHILI"
  ]
}
```

返回体建议：

```json
{
  "success": true,
  "code": "200",
  "message": "推荐成功",
  "data": {
    "recognizedIngredients": [
      "EGG",
      "TOMATO",
      "SCALLION"
    ],
    "recipes": [
      {
        "recipeName": "西红柿炒鸡蛋",
        "score": 0.95,
        "reason": "主食材齐全，适合快速制作",
        "missingIngredients": [],
        "steps": [
          "鸡蛋打散备用",
          "番茄切块",
          "先炒鸡蛋后下番茄翻炒",
          "加入葱花调味"
        ],
        "tips": [
          "番茄可先加少量糖提鲜"
        ],
        "difficulty": "EASY",
        "cookTimeMinutes": 10
      }
    ]
  }
}
```

## 7. DDL 初稿

### 6.1 人脸模板表

```sql
CREATE TABLE `sy_user_face_template` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `tenant_id` bigint DEFAULT NULL,
  `face_template_code` varchar(64) NOT NULL,
  `embedding_cipher_text` longtext NOT NULL,
  `embedding_dimension` int NOT NULL,
  `embedding_version` varchar(32) DEFAULT NULL,
  `quality_score` decimal(6,4) DEFAULT NULL,
  `liveness_score` decimal(6,4) DEFAULT NULL,
  `source_image_url` varchar(500) DEFAULT NULL,
  `image_sha256` varchar(128) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'ENABLE',
  `last_verified_time` datetime DEFAULT NULL,
  `ext` longtext DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_id` bigint DEFAULT NULL,
  `create_name` varchar(128) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_id` bigint DEFAULT NULL,
  `update_name` varchar(128) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` int DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_face_template_code` (`face_template_code`),
  UNIQUE KEY `uk_face_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 6.2 图像分析任务表

```sql
CREATE TABLE `image_analysis_task` (
  `id` bigint NOT NULL,
  `task_code` varchar(64) NOT NULL,
  `tenant_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `business_scene` varchar(64) NOT NULL,
  `source_image_url` varchar(500) DEFAULT NULL,
  `source_image_sha256` varchar(128) DEFAULT NULL,
  `task_status` varchar(32) NOT NULL,
  `detect_model` varchar(64) DEFAULT NULL,
  `detect_model_version` varchar(32) DEFAULT NULL,
  `agent_flow_code` varchar(64) DEFAULT NULL,
  `request_payload_json` longtext DEFAULT NULL,
  `response_payload_json` longtext DEFAULT NULL,
  `result_summary` varchar(1000) DEFAULT NULL,
  `cost_time_ms` bigint DEFAULT NULL,
  `error_message` varchar(500) DEFAULT NULL,
  `ext` longtext DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_id` bigint DEFAULT NULL,
  `create_name` varchar(128) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_id` bigint DEFAULT NULL,
  `update_name` varchar(128) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` int DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_image_task_code` (`task_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 6.3 图像检测结果表

```sql
CREATE TABLE `image_detection_record` (
  `id` bigint NOT NULL,
  `task_id` bigint NOT NULL,
  `label` varchar(64) NOT NULL,
  `class_code` varchar(64) NOT NULL,
  `confidence` decimal(6,4) NOT NULL,
  `left_x` int NOT NULL,
  `top_y` int NOT NULL,
  `box_width` int NOT NULL,
  `box_height` int NOT NULL,
  `area_ratio` decimal(8,6) DEFAULT NULL,
  `attributes_json` longtext DEFAULT NULL,
  `ext` longtext DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_id` bigint DEFAULT NULL,
  `create_name` varchar(128) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_id` bigint DEFAULT NULL,
  `update_name` varchar(128) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` int DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_detection_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## 8. 配置建议

```yaml
agent-helper:
  opencv:
    enabled: true
    face:
      threshold: 0.85
      quality-threshold: 0.80
      liveness-threshold: 0.90
      max-fail-count: 5
      lock-minutes: 10
    yolo:
      runtime: onnxruntime
      model-path: D:/models/yolo-kitchen-v1.onnx
      model-name: yolo-kitchen-v1
      confidence-threshold: 0.35
      iou-threshold: 0.45
      max-detections: 100
      input-width: 640
      input-height: 640
    kitchen:
      default-scene: KITCHEN_ASSISTANT
      max-recipe-count: 3
```

## 9. 开发顺序

1. 补 `opencv` 模块基础骨架
2. 补 `common` 三张表对应 entity/mapper/service
3. 实现厨房食材版 YOLO ONNX 推理与检测接口
4. 在 `agent` 增加菜谱推荐编排
5. 在 `user` 增加人脸绑定/登录/解绑/状态接口
6. 再补异常、日志、限流、测试

## 10. 第一批最小可运行范围

建议第一批只打通这条最小闭环：

1. 上传一张厨房食材图片
2. YOLO 识别 `EGG`、`TOMATO`、`POTATO`、`ONION`、`GARLIC` 等基础食材
3. Agent 输出 1 到 3 个推荐菜谱
4. 返回缺失配料和简版步骤

这样最容易做出可展示的小 demo。
