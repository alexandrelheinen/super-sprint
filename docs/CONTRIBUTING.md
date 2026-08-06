# Contributing to Super Sprint Supélec

Thank you for improving this project. This document defines the code standards for every language used in the repository and the workflow expected from human and automated contributors.

## Project overview

Super Sprint Supélec is a desktop Java Swing game inspired by [Super Sprint](http://www.giantbomb.com/super-sprint/3030-2776/). The codebase follows a **Model-View-Controller (MVC)** layout:

| Package     | Role                                      |
|-------------|-------------------------------------------|
| `model`     | Game state, physics, persistence          |
| `view`      | Swing UI (single AppShell window)         |
| `controller`| Game loop, input handling, AI logic       |

Sources live under `src/main/java/`; classpath resources under `src/main/resources/`; tests under `src/test/java/`. Documentation lives under `docs/`. Runtime Hall of Fame data is stored in an OS-specific user data directory (see `model.ResourcePaths`).

## General principles

1. **English everywhere in source code** - identifiers, comments, user-facing strings, and commit messages must be written in English. Historical French names are legacy debt; do not introduce new French identifiers.
2. **Minimal, focused changes** - match the style of surrounding code and avoid unrelated refactors in the same pull request.
3. **Keep the build green** - run `./gradlew test` and `./gradlew smokeTest` locally before opening a pull request. CI must pass.
4. **Preserve game behaviour** - refactors should not change gameplay unless explicitly requested.

## Java standards

### Version and build

- Target **Java 17** bytecode (CI uses Temurin 17; newer JDKs are fine for local builds).
- Build with the **Gradle Wrapper**: `./gradlew classes`, `./gradlew run`, `./gradlew test`.
- Do not commit `build/` or `.gradle/` outputs.
- A thin `Makefile` may wrap Gradle tasks; Gradle remains the source of truth.

### Naming conventions

Follow standard Java conventions ([Oracle Code Conventions](https://www.oracle.com/technetwork/java/codeconvtoc-136057.html)):

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
src/main/java/
  controller/   # Main, Game, Controller hierarchy, game tick task
  model/        # Car, Circuit, HallOfFame, Result, ResourcePaths
  view/         # AppShell, GameFrame (race canvas), screen panels
src/main/resources/
  sprites/      # Bundled PNG assets (classpath /sprites/…)
  data/         # Seed Hall of Fame + config properties (classpath /data/…)
src/test/java/  # JUnit 5 tests mirroring main packages
docs/           # Markdown documentation
```

Each public class belongs in its own file named after the class. The entry point is `controller.Main`.

### Resources

- Load assets and config via the classpath (`ResourcePaths`, `ConfigLoader`), never via working-directory-relative `src/…` paths.
- Build-generated sprites land in `build/generated/resources/main/` and are merged onto the runtime classpath / jar.

### Code organization

- **Model classes** must not import Swing view classes except where rendering coupling already exists (e.g. `Car` notifying `GameFrame`). Prefer reducing such coupling over time.
- **View classes** handle UI only; game rules belong in `model` or `controller`.
- **Controller classes** orchestrate the tick loop, player input, and AI decisions.
- Keep constants that describe game data (track tile maps, car stats, start positions) near the class that owns them, or in a dedicated constants class if shared.

### Formatting

- Indent with **tabs** (existing project style) or match the file you edit.
- Opening brace on the same line for methods and control structures.
- One statement per line; avoid deeply nested logic - extract private methods when clarity improves.
- Limit line length to ~120 characters where practical.

### Comments and documentation

- Write comments in English.
- Explain *why*, not *what*, for non-obvious logic (e.g. PD controller tuning, finish-line crossing detection).
- Avoid commented-out code in commits; delete dead code instead.
- Public APIs do not require Javadoc for this project, but brief class-level comments are welcome for complex components (`AiController`, `Circuit`).

### Error handling

- Do not swallow exceptions silently. Log to `System.err` or show a `JOptionPane` for user-facing failures.
- Prefer specific exception types over bare `catch (Exception)` in new code.
- Resource streams must be closed; use try-with-resources in new code.

### Swing and threading

- All UI updates run on the **Event Dispatch Thread (EDT)**. The game loop uses `java.util.Timer`; timer callbacks must not perform long blocking work.
- Use `SwingUtilities.invokeLater` when launching the app from non-UI threads.

### Serialization

- `Result` and `HallOfFame` persist leaderboard data via Java serialization to the user data file resolved by `ResourcePaths.userHallOfFameFile()` (seeded from classpath `/data/hall_of_fame.dat` on first run). `Result` stores name, total duration, lap count, and car model index (`serialVersionUID` `3`); rankings use mean lap time. Older UID `2` files are incompatible and are replaced with defaults on load. When changing serializable classes, bump `serialVersionUID` intentionally and regenerate the seed with `HallOfFameSeedGenerator`.

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
./gradlew classes      # must succeed with no errors
./gradlew test         # JUnit 5 suite under src/test/java
./gradlew smokeTest    # launches the app headlessly and exits cleanly
```

Unit tests mirror the main package structure (`model`, `controller`, `view`). They use JUnit 5 from Maven Central via Gradle. Tests must stay headless (no Swing windows) so they can run in CI.

## Asset and documentation files

| Path | Purpose |
|------|---------|
| `src/main/resources/sprites/` | PNG sprites and track tiles (classpath) |
| `third_party/kenney-…/` | Kenney zip + license (extracted at build) |
| `src/main/resources/data/hall_of_fame.dat` | Seed leaderboard copied on first run |
| `docs/REPORT.md` | English project report |
| `README.md` (repo root) | User-facing quick start |
| `docs/BUILD.md` | Build and run instructions |

Do not commit OS junk (`Thumbs.db`, `.DS_Store`). Binary assets should stay unchanged unless replacing art.

## Markdown documentation

- User docs (`README.md` at repo root) - concise quick start, build/run commands, controls.
- Technical report (`REPORT.md`) - architecture, design choices, references the original French submission when applicable.
- This file (`CONTRIBUTING.md`) - contributor and code standards.

Use GitHub-flavored Markdown, English prose, and fenced code blocks with language tags for commands.

## AI-assisted contributions

Automated coding agents (Cursor, Claude Code, Copilot Workspace, etc.) **must read this file before editing source code** and follow every rule above. Agents should:

1. Read `CONTRIBUTING.md` (this file) and `AGENTS.md`.
2. Run `./gradlew classes` / `./gradlew test` after Java changes.
3. Prefer English renames over adding bilingual comments.
4. Never commit secrets, tokens, or generated `build/` / `.gradle/` output.
5. Ask before destructive operations (deleting leaderboard data, rewriting history).

See [`AGENTS.md`](AGENTS.md) for the short agent checklist.
