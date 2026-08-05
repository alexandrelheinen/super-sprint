# Super Sprint Supélec — Project Report (English)

> **About this document**
>
> This file is an English translation and modernization of the original French project report submitted for the Supélec *Projet Logiciel* evaluation in February 2015. The original PDF was delivered to the course instructors but was **not kept in this Git repository**. This `REPORT.md` reproduces the report’s intent and structure, improves clarity where the source writing was idiomatic or imprecise, and adds notes about the current build system and CI workflow introduced in 2026.
>
> **Original submission:** French PDF report, Software Project Sequence 6, 2014/2015 academic year.  
> **Reference implementation:** commit history through the 2015 evaluation; refactored source under `src/controller`, `src/model`, and `src/view`.

---

## 1. Introduction

### 1.1 Context

This project was carried out as part of the Supélec engineering curriculum (*Projet Logiciel*, Sequence 6) between November 2014 and February 2015. The goal was to design and implement a complete desktop application in Java, from requirements analysis through to a playable prototype with documentation and UML models.

The chosen theme is a simplified clone of the arcade game **Super Sprint**: a top-down car race on modular tracks, with lap counting, collisions, and a persistent leaderboard.

### 1.2 Objectives

The project aimed to:

1. Deliver a **functional game** with menus, race setup, real-time rendering, and scoring.
2. Apply **object-oriented design** with clear separation of concerns.
3. Implement **non-trivial behaviour**: car physics, track boundary detection, AI opponents, and serialized persistence.
4. Produce **supporting artefacts**: UML class diagram, user documentation, and a written report (original French PDF).

### 1.3 Scope and limitations

The implementation deliberately focuses on core gameplay:

- Fixed race length of **three laps**.
- Up to **four cars** per race (human and/or AI).
- **Four track layouts** built from reusable tile types.
- **No network multiplayer**, sound, or advanced particle effects.
- Leaderboard stored in the Linux user data directory (`~/.local/share/super-sprint-supelec/` by default).

These constraints kept the project achievable within the academic schedule while still covering analysis, design, and integration work.

---

## 2. Functional specification

### 2.1 User workflow

1. **Main menu** — choose single-player, two-player, Hall of Fame, or help.
2. **Race setup** — select car model(s), track, then start the race.
3. **Race** — real-time top-down view, lap counter, elapsed time, collisions.
4. **End of race** — if a human player finishes first within the lap limit, prompt for a Hall of Fame name when the time qualifies; otherwise show a message when the AI wins.
5. **Hall of Fame** — view top ten times per track.

### 2.2 Controls

| Player | Accelerate | Brake | Steer |
|--------|------------|-------|-------|
| 1 | ↑ | ↓ | ← / → |
| 2 | W | S | A / D |

Steering input only affects heading when the car is moving. Releasing accelerate/brake triggers friction-based deceleration until the car stops.

### 2.3 Car models

Four models differ by three statistics (stored in `Car.CAR_MODEL_STATS`):

| Model | Acceleration | Top speed | Handling |
|-------|-------------|-----------|----------|
| 1 | 120 | 280 | 50 |
| 2 | 200 | 320 | 38 |
| 3 | 146 | 374 | 40 |
| 4 | 170 | 250 | 55 |

Higher handling increases turn rate; top speed is enforced each physics step.

### 2.4 Tracks

Each track is a grid of **tile types** (values 1–7) mapped to PNG assets under `src/sprites/` (`trackN.png`). Types represent straights, corners, and special pieces. Four predefined layouts are stored in `Game.TRACK_MAPS`. Start positions are defined per track in `Circuit.START_POSITIONS`.

The finish line is a geometric segment near the first grid slot; crossing direction determines valid lap increments.

---

## 3. Software architecture

### 3.1 MVC organization

The codebase follows **Model–View–Controller**:

| Layer | Responsibility | Main classes |
|-------|----------------|--------------|
| **Model** | Game state, rules, persistence | `Car`, `Circuit`, `HallOfFame`, `Result` |
| **View** | Swing UI and rendering | `MenuFrame`, `GameFrame`, `HallFrame` |
| **Controller** | Loop, input, AI | `Game`, `Controller`, `HumanController`, `AiController`, `GameTickTask`, `Main` |

The UML class diagram is maintained in `diagram/classes.ucls` (ObjectAid) with PNG export `diagram/classes.png`.

### 3.2 Observer pattern

`Car` and `Circuit` extend `java.util.Observable` and notify `GameFrame` when state changes. `HallOfFame` notifies `HallFrame` when rankings change. This was a standard Swing-era pattern taught in the course; it is deprecated in modern Java but preserved for historical fidelity until a listener-based refactor is undertaken.

### 3.3 Game loop

`Game` starts a `java.util.Timer` that fires every **10 ms** (`Game.TICK_INTERVAL_MS`). Each tick (`GameTickTask`):

1. Advances circuit time and notifies the view.
2. Checks whether any car exceeded the lap count (race end).
3. For each controller: enforce track boundaries, update car physics, resolve pairwise collisions.

Human input is handled on the EDT via `KeyListener` on `GameFrame`. AI logic runs inside the timer callback.

---

## 4. Detailed design

### 4.1 Car physics (`Car`)

State includes position `(x, y)`, heading `angle`, speed, acceleration, and motion state (idle, accelerating, coasting/braking).

Each step (`applyPhysics`):

- Applies friction when no throttle input is held.
- Clamps speed to the model’s maximum.
- Integrates position using `v * cos/sin(angle)`.
- Updates lap count via `Circuit.crossFinishLine`.

Collisions (`collideWith`) use axis-aligned rectangles rotated per car, then adjust speeds and headings with a simple elastic blend constant.

### 4.2 Track logic (`Circuit`)

`enforceTrackBoundaries` maps the car to grid coordinates and checks whether the pixel position lies inside the valid corridor for that tile type:

- **Types 1–2:** straight segments with inner/outer radius bounds.
- **Types 3–6:** quarter-circle corners around tile corners.
- **Type 7:** open tile (no extra constraint).

Leaving the corridor reverses part of the speed and nudges the car back toward the tile centre.

### 4.3 AI controller (`AiController`)

AI opponents always accelerate and steer using a **PD (proportional–derivative) controller** on heading error:

- The **reference heading** depends on the current tile type and the previous grid cell (inferring driving direction through corners).
- Gains scale with the car’s handling statistic.
- Output is saturated to limit turn rate.

This approach was chosen as a lightweight alternative to full path planning, sufficient for arcade opponents on fixed tracks.

### 4.4 Hall of Fame (`HallOfFame`, `Result`)

Each track keeps ten best times. `Result` stores player name, time in milliseconds, and timestamp. On Linux, data is serialized to `$XDG_DATA_HOME/super-sprint-supelec/hall_of_fame.dat` (default `~/.local/share/super-sprint-supelec/hall_of_fame.dat`), seeded from `src/data/hall_of_fame.dat` on first run. On first run failure (or corrupt file), default placeholder entries are created.

When a human player wins, `Game` passes the race time to `tryAddResult`; if the AI wins, an artificially large time is recorded so human entries are not overwritten spuriously.

### 4.5 View layer

- **`MenuFrame`** — main and pre-race menus, car/track selectors, launches `Game`.
- **`GameFrame`** — double-buffered rendering of track tiles, cars, HUD (lap counts, race timer, finish line).
- **`HallFrame`** — tabular leaderboard with per-track selection.

All user-visible strings were translated to English during the 2026 refactor; the original UI was French.

---

## 5. Implementation notes

### 5.1 Technology stack

- **Language:** Java (originally Java 7/8 era; currently validated on **JDK 17+**).
- **UI:** Swing (`JFrame`, `JComboBox`, `JTable`, …).
- **Build (2015):** manual `javac` invocation.
- **Build (current):** GNU Make (`Makefile`), see [BUILD.md](BUILD.md).
- **CI (current):** GitHub Actions workflow compiling and smoke-launching the app headlessly with Xvfb.

### 5.2 Asset pipeline

Graphics are static PNG files under `src/sprites/`:

- Car sprites: `car1.png` … `car4.png`
- Track tiles: `track1.png` … `track7.png`
- UI: `menu.png`, `icon.png`, `track_preview1.png` … `track_preview4.png`, background texture

Tile size in pixels is `GameFrame.TILE_SIZE` (219 px).

### 5.3 Entry point

```bash
make run
# equivalent: java -cp build controller.Main
```

`Main` constructs `MenuFrame`, which bootstraps the Hall of Fame and menu UI.

---

## 6. Testing and validation

### 6.1 Original testing (2015)

Validation was primarily **manual**: play-through on each track, two-player input, Hall of Fame read/write, and AI completion of laps. No automated unit tests were part of the course deliverable.

### 6.2 Current automated checks (2026)

The CI pipeline verifies:

1. **Compilation** — all sources under `src/` compile without errors.
2. **Launch smoke test** — the JVM starts, Swing initializes under a virtual framebuffer, and the process remains alive for several seconds without crashing.

Contributors should run `make compile` and `make smoke-test` before opening pull requests (see [CONTRIBUTING.md](CONTRIBUTING.md)).

---

## 7. Known issues and future work

Issues present in the original submission and still relevant:

| Topic | Description |
|-------|-------------|
| Deprecated `Observable` | Should migrate to explicit listeners or a game-state bus. |
| Tight view coupling | Model classes notify Swing frames directly. |
| AI tuning | PD gains are heuristic; AI can cut corners or stall on some tiles. |
| Collision model | Rotated-rectangle approximation is imprecise at high speed. |
| Serialization | User Hall of Fame file breaks if `Result` fields change without migration. |
| Headless CI | Smoke test confirms startup only, not gameplay correctness. |

Possible extensions: unit tests for physics and lap detection, replays, additional tracks, sound, modern build tool (Gradle), and replacing serialization with JSON.

---

## 8. Conclusion

The Super Sprint Supélec project successfully met its academic goals: a playable arcade racer with menus, AI opponents, persistence, and documented object-oriented structure. The MVC split made it possible to evolve rendering and control logic independently of core physics.

The 2026 maintenance work added contributor standards, a reproducible Makefile build, CI smoke testing, English naming throughout the source tree, and this translated report so the project remains approachable without the original French PDF.

---

## 9. References

1. Original French PDF project report (2015 evaluation submission — not stored in this repository).
2. Course materials: Supélec *Projet Logiciel*, Sequence 6, 2014/2015.
3. Inspiration: *Super Sprint* (arcade, 1985).
4. Repository documentation: [README.md](../README.md), [BUILD.md](BUILD.md), [CONTRIBUTING.md](CONTRIBUTING.md).
5. UML diagram: [diagram/classes.ucls](diagram/classes.ucls), [diagram/classes.png](diagram/classes.png).

---

## Appendix A — Class inventory (current packages)

| Class | Package | Role |
|-------|---------|------|
| `Main` | controller | Application entry point |
| `Game` | controller | Race session orchestration |
| `Controller` | controller | Abstract player/AI binding to `Car` |
| `HumanController` | controller | Keyboard input |
| `AiController` | controller | PD steering AI |
| `GameTickTask` | controller | Timer tick callback |
| `Car` | model | Physics and rendering notifications |
| `Circuit` | model | Track grid, boundaries, timing |
| `HallOfFame` | model | Leaderboard persistence |
| `Result` | model | Serializable score entry |
| `MenuFrame` | view | Menus |
| `GameFrame` | view | Race rendering |
| `HallFrame` | view | Leaderboard UI |

## Appendix B — Glossary (French → English)

Terms used in the original codebase and report:

| French (legacy) | English (current) |
|-----------------|-------------------|
| controleur | controller |
| modele | model |
| vue | view |
| voiture | car |
| mettre à jour | update |
| circuit | circuit / track |
| tours | laps |
| Hall of Fame | Hall of Fame (unchanged) |
