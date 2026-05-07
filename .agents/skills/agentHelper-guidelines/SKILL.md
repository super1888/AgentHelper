---
name: agenthelper-guidelines
description: SpringAi/AgentHelper repository rules for module placement, backend API style, page structure, comments, validation, and change boundaries. Use when working in this repo on backend features, UI pages, WebSocket flows, bug fixes, or any module-level implementation.
---

# AgentHelper Guidelines

Use these rules when working inside this repository. Do not apply generic Spring Boot habits blindly.

## Repository Layout

This is a multi-module Maven project.

- `quickStart`: startup module, page entry, static assets, unified web config
- `common`: shared constants, exceptions, common configs, shared response objects
- `core`: core capabilities
- `agent`: agent business logic
- `vectorStore`: vector storage capabilities
- `user`: user, tenant, auth, and login-related logic
- `graph`: workflow and graph-related capabilities
- `hooks`, `interceptors`, `tools`, `websocket`, `a2a`: extension modules
- `docs`: project documentation
- `ui`: frontend workspace or standalone frontend assets, not the default runtime entry

## Ownership Rules

- Put general reusable code in `common`
- Put business-specific code in the matching business module
- Do not dump everything into `quickStart` just to save time
- If logic is reusable string/data processing, move it into `common` utilities instead of leaving it inside manager/controller classes
- If tenant parsing, string normalization, digest generation, or similar cross-module helpers already exist in `common`, call the shared helper instead of re-implementing methods such as `resolveTenantName`, `resolveTenantId`, `normalize`, or `sha256`
- If code is only responsible for assembling response objects, place it in the module `application/assmbler` package
- For custom agent flows in the `agent` module, keep `ApplicationManager` focused on orchestration and move stage-specific logic into dedicated services
- For custom agent internal step data, use explicit DTO classes in `agent/domain/dto` rather than private inner classes in manager files
- Cross-cutting enterprise logging should live in an independent `logging` module, while the runtime entry module keeps only startup-level config such as `logback-spring.xml`

## Backend Rules

- Use `@RestController`
- Prefer `jakarta.annotation.Resource` for injection
- Return `ApiResponse.success(...)` or `ApiResponse.fail(...)` consistently
- Keep controller logic thin: request parsing and response wrapping only
- Put business logic in `Service`
- Validate parameters early: null, range, boundary, and illegal values must be handled

## API Rules

- `quickStart` adds `/agentHelper` automatically to all `@RestController` endpoints via `ApiPrefixWebConfig`
- Only declare the module-relative path in code
- Do not duplicate `/agentHelper` in controller mappings
- Prefer dedicated response classes over raw `Map`
- Reuse existing response and exception styles when possible
- For built-in custom agents that do not require users to create agent records manually, expose dedicated endpoints under module-owned controllers instead of forcing them through the generic simple-agent creation flow

## Page Rules

- Use Vue 3 for frontend work
- Keep page style consistent, clear, and production-ready
- Use real request paths with the `/agentHelper/...` prefix
- When adding built-in custom agent capabilities, prefer embedding them into the existing management page as a dedicated panel/window before creating a new standalone route
- If the backend supports per-stage model selection, the UI should expose stage-level model pickers and clearly show which model each stage actually used

## Logging Rules

- Use Spring Boot default Logback for enterprise logging unless the user explicitly asks for another stack
- Do not introduce `slf4j-simple` or other lightweight console-only bindings into this repo
- Keep log path, rolling size, retention days, total size cap, console switch, and trace header settings configurable in YAML
- Prefer separate rolling files for app logs, debug logs, and error logs to simplify online troubleshooting
- Add request-level `traceId` through a shared filter so backend logs can be correlated across controllers and services
- Log configuration should support file rolling by size and date, and should keep storage bounded

## Style Rules

- Java indentation: 4 spaces
- HTML/CSS/JS indentation: 2 spaces
- Use meaningful names for variables, methods, and classes
- Use camelCase for variables and functions
- Use UPPER_SNAKE_CASE for constants
- Avoid names like `a`, `b`, `tmp`, `test1`
- Keep methods single-purpose
- Avoid long methods and oversized classes
- Keep folder structure aligned with existing module structure

## Comment Rules

Add Chinese comments when the logic is non-trivial.

- File header: purpose and core responsibility
- Public methods: add Chinese comments above them
- Complex logic blocks: explain the intent in Chinese
- Page styles: comment by region/module when needed

Do not add meaningless comments.

- Do not write comments like "define variable" or "execute method"
- Do not mix Chinese and English in one comment
- Do not end with only `TODO`

## Data And Response Rules

- Prefer dedicated response objects over raw `Map`
- Keep request parameters clear
- Keep response structure stable
- User-facing backend success and error messages must be returned in Chinese unless the user explicitly requires another language
- Error messages must be readable and actionable
- For multi-stage custom agent flows, return structured stage results so the frontend can render stage name, status, model binding, summary, issues, and output content
- For stage-level model overrides, keep a default model field and let unset stages fall back to that default model

## Validation Rules

Before submitting, confirm:

1. Code is in the correct module
2. Endpoint paths do not repeat `/agentHelper`
3. Page resource paths match `ClassPathResource` usage
4. Empty, failed, and boundary states are handled
5. No obvious dead code or unimplemented logic remains
6. If it cannot compile, state the blocking reason clearly
7. If adding custom agent orchestration, confirm manager/service/assembler responsibilities are separated cleanly
8. If extracting common helpers or DTOs, confirm package ownership is coherent and no avoidable coupling is introduced

## Current Stack

- Spring Boot `3.5.8`
- Spring AI `1.1.2`
- Multi-module Maven

## Default Workflow

When working in this repo:

1. Identify the owning module first
2. Check existing controller/service/domain/style patterns
3. Reuse shared constants, response objects, and exception handling
4. Make backend and frontend changes runnable
5. Self-check and report validation results and blockers

If the user says "follow this repo style", use this skill as the default constraint.
