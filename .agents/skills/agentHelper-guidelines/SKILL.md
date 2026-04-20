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

## Page Rules

- Use Vue 3 for frontend work
- Keep page style consistent, clear, and production-ready
- Use real request paths with the `/agentHelper/...` prefix

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
- Error messages must be readable and actionable

## Validation Rules

Before submitting, confirm:

1. Code is in the correct module
2. Endpoint paths do not repeat `/agentHelper`
3. Page resource paths match `ClassPathResource` usage
4. Empty, failed, and boundary states are handled
5. No obvious dead code or unimplemented logic remains
6. If it cannot compile, state the blocking reason clearly

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
