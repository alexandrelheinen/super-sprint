# Instructions for AI coding agents

This repository is a Java Swing game (**Super Sprint Supélec**). Before making any code change, read and follow **[CONTRIBUTING.md](CONTRIBUTING.md)** — it is the single source of truth for naming, package layout, formatting, and review expectations.

## Required reading order

1. **[CONTRIBUTING.md](CONTRIBUTING.md)** — code standards, MVC structure, Java conventions, git workflow.
2. **[README.md](../README.md)** — how to build and run the application.
3. **[REPORT.md](REPORT.md)** — architecture and design context (when present).

## Quick checklist for agents

- [ ] Use English identifiers, comments, and UI strings.
- [ ] Respect MVC packages: `model`, `view`, `controller`.
- [ ] Build from repo root: `make compile`.
- [ ] Verify launch: `make smoke-test` (headless smoke test).
- [ ] Do not commit `build/` or `*.class` files.
- [ ] Match existing tab indentation and brace style in edited files.
- [ ] Keep pull requests focused; separate documentation, CI, and refactors when possible.

## Entry point

```
controller.Main
```

Run the game with `make run` from the repository root. Bundled assets load from `src/sprites/`; prepared car sprites may be written to `build/sprites/`.

## When unsure

Prefer minimal diffs, preserve gameplay behaviour, and document non-obvious changes in commit messages. If a task conflicts with CONTRIBUTING.md, follow CONTRIBUTING.md.
