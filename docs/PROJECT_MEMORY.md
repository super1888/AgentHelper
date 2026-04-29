# Project Memory

## Repository Overview

- Project path: `D:/code/springAi`
- Multi-module Maven Spring Boot project
- Parent stack:
  - Spring Boot `3.5.8`
  - Spring AI `1.1.2`

## Module Ownership

- `quickStart`: startup module, runtime entry, static assets, unified web config
- `common`: shared constants, exceptions, configs, shared response objects
- `core`: core capabilities
- `agent`: agent business logic
- `vectorStore`: vector storage capabilities
- `user`: user, tenant, auth, login-related logic
- `graph`: workflow and graph-related capabilities
- `hooks`, `interceptors`, `tools`, `websocket`, `a2a`: extension modules
- `ui`: frontend workspace, not the default runtime entry

## Backend Conventions

- Use `@RestController`
- Prefer `jakarta.annotation.Resource` for injection
- Keep controller logic thin
- Put business logic in `Service`
- Validate parameters early
- Return `ApiResponse.success(...)` or `ApiResponse.fail(...)` consistently

## API Conventions

- `quickStart` adds `/agentHelper` automatically through `ApiPrefixWebConfig`
- Controller mappings should only declare module-relative paths
- Do not duplicate `/agentHelper` in controller annotations

## Code Style

- Java indentation: 4 spaces
- HTML/CSS/JS indentation: 2 spaces
- Use meaningful names
- Keep methods single-purpose
- Avoid oversized classes

## Comment Conventions

- Add Chinese comments for non-trivial logic
- Add Chinese comments above public methods when needed
- Explain complex logic blocks in Chinese
- Avoid meaningless comments

## Logging Decisions

- Exclude `commons-logging` from `common` module `pdfbox` dependency
- Exclude `slf4j-simple` from `core` module `dashscope-sdk-java` dependency
- Configure `quickStart` `spring-boot-maven-plugin` with:
  - `mainClass = com.spring.quickstart.QuickStartApplication`
  - `fork = true`
- Parent `pom.xml` uses `maven-enforcer-plugin` to ban:
  - `commons-logging:commons-logging`
  - `org.slf4j:slf4j-simple`

## Known Issue

- `websocket/pom.xml` has an existing invalid dependency scope:
  - `com.spring.ai:user` uses scope `user`
- This issue is unrelated to the logging fix, but it can break full Maven reactor builds

## Preferred Startup Context

- Default application entry: `com.spring.quickstart.QuickStartApplication`
- If using Maven startup, prefer forked Spring Boot startup to avoid Maven runtime logging jars leaking into the app process
