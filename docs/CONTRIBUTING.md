# Contributing to Super Sprint Supélec

Thank you for improving this project. This document defines the code standards for every language used in the repository and the workflow expected from human and automated contributors.

## Project overview

Super Sprint Supélec is a desktop Java Swing game inspired by [Super Sprint](http://www.giantbomb.com/super-sprint/3030-2776/). The codebase follows a **Model–View–Controller (MVC)** layout:

| Package     | Role                                      |
|-------------|-------------------------------------------|
| `model`     | Game state, physics, persistence          |
| `view`      | Swing UI (single AppShell window)         |
| `controller`| Game loop, input handling, AI logic       |

Assets live under `src/sprites/` and `src/data/`. Documentation lives under `docs/`. Runtime Hall of Fame data is stored in the Linux user data directory (see `model.ResourcePaths`).

## General principles

1. **English everywhere in source code** — identifiers, comments, user-facing strings, and commit messages must be written in English. Historical French names are legacy debt; do not introduce new French identifiers.
2. **Minimal, focused changes** — match the style of surrounding code and avoid unrelated refactors in the same pull request.
3. **Keep the build green** — run `make compile` and `make smoke-test` locally before opening a pull request. CI must pass.
4. **Preserve game behaviour** — refactors should not change gameplay unless explicitly requested.

## Java standards

### Version and build

- Target **Java 17** or newer (CI uses Temurin 17).
- Compile with `make compile`; run with `make run` from the repository root so asset paths resolve correctly.
- Class files are emitted to `build/` (never commit `.class` files).

### Naming conventions

Follow standard Java conventions ([Oracle Code Conventions](https://www.oracle.com/java/technologies/javase/codeconventions-namingconventions.html)):

| Element            | Style              | Examples                                      |
|--------------------|--------------------|-----------------------------------------------|
| Packages           | lowercase          | `controller`, `model`, `view`                 |
| Classes / interfaces | PascalCase       | `Car`, `GameFrame`, `AiController`            |
| Methods / variables | camelCase         | `applyPhysics`, `lapCount`, `trackIndex`      |
| Constants          | UPPER_SNAKE_CASE   | `TICK_INTERVAL_MS`, `CAR_MODEL_COUNT`         |
| Enum constants     | UPPER_SNAKE_CASE   | `STRAIGHT_HORIZONTAL`                         |

**Do not use:**

- French words in identifiers (`voiture`, `mettreAJour`, `controleur`, …).
- Abbreviations that hurt readability (`numCirc` → prefer `trackCount`).
- Single-letter names except for short loop indices (`i`, `j`, `k`).

### Package layout

```
src/
  controller/   # Main, Game, Controller hierarchy, game tick task
  model/        # Car, Circuit, HallOfFame, Result, ResourcePaths
  view/         # AppShell, GameFrame (race canvas), screen panels
  sprites/      # Bundled PNG assets
  data/         # Seed Hall of Fame serialization file
docs/           # Markdown documentation
```

Each public class belongs in its own file named after the class. The entry point is `controller.Main`.

### Code organization

- **Model classes** must not import Swing view classes except where rendering coupling already exists (e.g. `Car` notifying `GameFrame`). Prefer reducing such coupling over time.
- **View classes** handle UI only; game rules belong in `model` or `controller`.
- **Controller classes** orchestrate the tick loop, player input, and AI decisions.
- Keep constants that describe game data (track tile maps, car stats, start positions) near the class that owns them, or in a dedicated constants class if shared.

### Formatting

- Indent with **tabs** (existing project style) or match the file you edit.
- Opening brace on the same line for methods and control structures.
- One statement per line; avoid deeply nested logic — extract private methods when clarity improves.
- Limit line length to ~120 characters where practical.

### Comments and documentation

- Write comments in English.
- Explain *why*, not *what*, for non-obvious logic (e.g. PD controller tuning, finish-line crossing detection).
- Avoid commented-out code in commits; delete dead code instead.
- Public APIs do not require Javadoc for this project, but brief class-level comments are welcome for complex components (`AiController`, `Circuit`).

### Error handling

- Do not swallow exceptions silently. Log to `System.err` or show a `JOptionPane` for user-facing failures.
- Prefer specific exception types over bare `catch (Exception)` in new code.
- Resource streams (`FileInputStream`, `ObjectInputStream`) must be closed; use try-with-resources in new code.

### Swing and threading

- All UI updates run on the **Event Dispatch Thread (EDT)**. The game loop uses `java.util.Timer`; timer callbacks must not perform long blocking work.
- Use `SwingUtilities.invokeLater` when launching the app from non-UI threads.

### Serialization

- `Result` and `HallOfFame` persist leaderboard data via Java serialization to the user data file resolved by `ResourcePaths.userHallOfFameFile()` (seeded from `src/data/hall_of_fame.dat` on first run). `Result` stores name, total duration, and lap count (`serialVersionUID` `2`); rankings use mean lap time. When changing serializable classes, bump `serialVersionUID` intentionally and document migration needs.

### Deprecated APIs

The project uses `java.util.Observable` / `Observer` (deprecated since Java 9). New features should not extend this pattern; a future migration to property-change listeners or manual callbacks is acceptable.

## Git workflow

1. Branch from `master` using the prefix `cursor/` for automated agent work or a descriptive name for human work.
2. Write commit messages in English, imperative mood (`Add build workflow`, `Rename Car model fields`).
3. Keep commits logically separated (docs, build, CI, refactor, README).
4. Open a pull request against `master` and ensure CI is green.

## Testing expectations

Validation is:

```bash
make compile      # must succeed with no errors
make test         # runs the JUnit 5 suite in tests/
make smoke-test   # launches the app headlessly and exits cleanly
```

Unit tests live under `tests/`, mirroring the main package structure (`tests/model/`, `tests/controller/`). They use JUnit 5; `make test` downloads the JUnit console launcher into `build/lib/` on first run. Tests must stay headless (no Swing windows) so they can run in CI.

## Asset and documentation files

| Path                        | Purpose                                      |
|-----------------------------|----------------------------------------------|
| `src/sprites/`              | PNG sprites and track tiles                  |
| `third_party/kenney-…/`     | Kenney zip + license (extracted at build)    |
| `src/data/hall_of_fame.dat` | Seed leaderboard copied on first run         |
| `docs/REPORT.md`            | English project report                       |
| `README.md` (repo root)     | User-facing quick start                      |
| `docs/BUILD.md`             | Build and run instructions                   |

Do not commit OS junk (`Thumbs.db`, `.DS_Store`). Binary assets should stay unchanged unless replacing art.

## Markdown documentation

- User docs (`README.md` at repo root) — concise quick start, build/run commands, controls.
- Technical report (`REPORT.md`) — architecture, design choices, references the original French submission when applicable.
- This file (`CONTRIBUTING.md`) — contributor and code standards.

Use GitHub-flavored Markdown, English prose, and fenced code blocks with language tags for commands.

## AI-assisted contributions

Automated coding agents (Cursor, Claude Code, Copilot Workspace, etc.) **must read this file before editing source code** and follow every rule above. Agents should:

1. Read `CONTRIBUTING.md` (this file) and `AGENTS.md`.
2. Run `make compile` after Java changes.
3. Prefer English renames over adding bilingual comments.
4. Never commit secrets, tokens, or generated `build/` output.
5. Ask before destructive operations (deleting leaderboard data, rewriting history).

See [`AGENTS.md`](AGENTS.md) for the short agent checklist.
