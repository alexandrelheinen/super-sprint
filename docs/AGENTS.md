# Instructions for AI coding agents

This repository is a Java Swing game (**Super Sprint Supélec**) built with **Gradle** and the standard `src/main` / `src/test` layout. Before making any code change, read and follow **[CONTRIBUTING.md](CONTRIBUTING.md)** - it is the single source of truth for naming, package layout, formatting, and review expectations.

## Required reading order

1. **[CONTRIBUTING.md](CONTRIBUTING.md)** - code standards, MVC structure, Java conventions, git workflow.
2. **[README.md](../README.md)** / **[BUILD.md](BUILD.md)** - how to build and run the application.
3. **[REPORT.md](REPORT.md)** - architecture and design context (when present).

## Quick checklist for agents

- [ ] Use English identifiers, comments, and UI strings.
- [ ] Respect MVC packages: `model`, `view`, `controller` under `src/main/java/`.
- [ ] Build with `./gradlew classes` (or `./gradlew test`).
- [ ] Verify launch: `./gradlew smokeTest` (headless smoke test).
- [ ] Load assets via classpath (`ResourcePaths` / `ConfigLoader`), not filesystem `src/…` paths.
- [ ] Do not commit `build/` or `.gradle/` output.
- [ ] Match existing tab indentation and brace style in edited files.
- [ ] Keep pull requests focused; separate documentation, CI, and refactors when possible.

## Entry point

```
controller.Main
```

Run the game with `./gradlew run`. Bundled assets load from the classpath under `/sprites/` and `/data/`; derived car/Kenney/preview sprites are generated into `build/generated/resources/main/` at build time.

## When unsure

Prefer minimal diffs, preserve gameplay behaviour, and document non-obvious changes in commit messages. If a task conflicts with CONTRIBUTING.md, follow CONTRIBUTING.md.
