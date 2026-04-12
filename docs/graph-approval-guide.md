# Graph 审批工作流编排指南

本文基于 `https://java2ai.com/docs/frameworks/graph-core/quick-start` 的思路，在当前项目里落了一套可直接调接口学习的审批工作流示例。

目标不是做一个“最复杂”的审批系统，而是做一条你能快速看懂、能运行、能中断、能恢复、能发邮件推送的 Graph 样例。

## 一、这套示例放在哪里

- 控制层：`quickStart/src/main/java/com/spring/quickstart/workflow/approval/controller/ApprovalWorkflowController.java`
- Graph 配置：`quickStart/src/main/java/com/spring/quickstart/workflow/approval/config/ApprovalWorkflowGraphConfig.java`
- 节点实现：`quickStart/src/main/java/com/spring/quickstart/workflow/approval/node/`
- 服务实现：`quickStart/src/main/java/com/spring/quickstart/workflow/approval/service/impl/ApprovalWorkflowServiceImpl.java`
- 邮件集成：`quickStart/src/main/java/com/spring/quickstart/workflow/approval/service/ApprovalMailService.java`

## 二、流程设计

当前审批 Graph 的节点顺序如下：

1. `validateRequest`
说明：校验请求参数，并初始化审批单状态。

2. `prepareApproval`
说明：整理审批摘要，把后续发给审批人的核心信息提前准备好。

3. `notifyApprover`
说明：调用现有 `EmailUtil` 给审批人发待审批邮件。

4. `approvalDecision`
说明：人工审批节点。
这里没有直接执行，而是在编译 Graph 时通过 `interruptBefore("approvalDecision")` 让流程在进入该节点前暂停。

5. `notifyApprovedApplicant` / `notifyRejectedApplicant`
说明：根据审批结果通知申请人。

## 三、为什么要用 interruptBefore

文档里的核心思想之一是：已知的人工介入点，适合用 `interruptBefore`。

这次审批场景就非常典型：

- 推送邮件发完之后，流程必须等待审批人动作
- 审批动作来自外部接口，不在节点内部自动完成
- 所以最适合在 `approvalDecision` 节点前中断

这样做的好处是：

- Graph 会自动保存当前状态
- 你可以随时通过接口查询当前状态
- 审批接口拿到结果后，只需要 `updateState()` 再继续跑

## 四、当前示例如何恢复执行

发起审批后，服务端会：

1. 创建 `threadId`
2. 用 `threadId` 启动 Graph
3. 流程执行到 `approvalDecision` 前自动中断

人工审批接口会：

1. 根据 `threadId` 找到这条流程
2. 调用 `approvalWorkflowGraph.updateState(...)`
3. 把 `approvalDecision` 和 `approvalComment` 写回状态
4. 再次调用 `stream(null, updatedConfig)` 从中断点继续执行

## 五、接口说明

### 1. 发起审批

`POST /workflow/approval/start`

请求示例：

```json
{
  "requestNo": "REQ-20260412-001",
  "applicantName": "张三",
  "applicantEmail": "zhangsan@example.com",
  "department": "研发中心",
  "reason": "请假并同步项目交接安排",
  "leaveDays": 2,
  "amount": 1000,
  "approverName": "李经理",
  "approverEmail": "manager@example.com"
}
```

### 2. 审批并恢复执行

`POST /workflow/approval/{threadId}/approve`

请求示例：

```json
{
  "approved": true,
  "comment": "同意，请安排好工作交接"
}
```

### 3. 查询当前状态

`GET /workflow/approval/{threadId}`

## 六、建议你怎么学习

1. 先发起审批，观察返回的 `threadId` 和 `timeline`
2. 再查询一次，确认流程停在等待审批
3. 调审批接口传 `approved=true`
4. 再查询一次，看流程已经继续跑到结束
5. 用 `approved=false` 再走一遍，对比状态变化
