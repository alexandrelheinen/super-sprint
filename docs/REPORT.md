# Super Sprint Supélec - Project Report (English)

> **About this document**
>
> This file is an English translation and modernization of the original French project report submitted for the Supélec *Projet Logiciel* evaluation in February 2015. The original PDF was delivered to the course instructors but was **not kept in this Git repository**. This `REPORT.md` reproduces the report’s intent and structure, improves clarity where the source writing was idiomatic or imprecise, and adds notes about the current build system and CI workflow introduced in 2026.
>
> **Original submission:** French PDF report, Software Project Sequence 6, 2014/2015 academic year.  
> **Reference implementation:** commit history through the 2015 evaluation; refactored source under `src/controller`, `src/model`, and `src/view`.

## 1. Introduction

### 1.1 Context

This project was carried out as part of the Supélec engineering curriculum (*Projet Logiciel*, Sequence 6) between November 2014 and February 2015. The goal was to design and implement a complete desktop application in Java, from requirements analysis through to a playable prototype with documentation and UML models.

The chosen theme is a simplified clone of the arcade game **Super Sprint**: a top-down car race on modular tracks, with lap counting, collisions, and a persistent leaderboard.

<video src="https://github.com/alexandrelheinen/super-sprint/releases/download/v2.0/dune-horseshoe-ai-demo.mp4" controls width="720"></video>

[Demo: four AI cars on Dune Horseshoe](https://github.com/alexandrelheinen/super-sprint/releases/download/v2.0/dune-horseshoe-ai-demo.mp4)

### 1.2 Objectives

The project aimed to:

1. Deliver a **functional game** with menus, race setup, real-time rendering, and scoring.
2. Apply **object-oriented design** with clear separation of concerns.
3. Implement **non-trivial behaviour**: car physics, track boundary detection, AI opponents, and serialized persistence.
4. Produce **supporting artefacts**: user documentation and a written report (original French PDF).

### 1.3 Scope and limitations

The implementation deliberately focuses on core gameplay:

- Fixed race length of **three laps**.
- Up to **four cars** per race (human and/or AI).
- **Four track layouts** built from reusable tile types.
- **No network multiplayer**, sound, or advanced particle effects.
- Leaderboard stored in the Linux user data directory (`~/.local/share/super-sprint-supelec/` by default).

These constraints kept the project achievable within the academic schedule while still covering analysis, design, and integration work.

## 2. Functional specification

### 2.1 User workflow

1. **Main menu** - choose single-player, two-player, Hall of Fame, or help.
2. **Race setup** - select car model(s), track, then start the race.
3. **Race** - real-time top-down view, lap counter, elapsed time, collisions.
4. **End of race** - show a race completion window with the winner, total time, and mean-lap leaderboard placement; human winners who place can enter a name.
5. **Hall of Fame** - view top ten mean lap times per track (name, duration, laps, mean, date).

### 2.2 Controls

| Player | Accelerate | Brake | Steer |
|--------|------------|-------|-------|
| 1 | ↑ | ↓ | ← / → |
| 2 | W | S | A / D |

Steering input only affects heading when the car is moving. Releasing accelerate/brake triggers friction-based deceleration until the car stops.

### 2.3 Car models

Nine liveries differ by three statistics (loaded from `cars.properties` into `Car.CAR_MODEL_STATS`):

| Model | Acceleration | Top speed | Handling |
|-------|-------------|-----------|----------|
| 1 | 120 | 280 | 50 |
| 2 | 200 | 320 | 38 |
| 3 | 146 | 374 | 40 |
| 4 | 170 | 250 | 55 |

Higher handling increases turn rate; top speed is enforced each physics step. With handling $h$ and scale $k_{\mathrm{turn}}$, the maximum yaw rate while moving is

$$
\omega_{\max} = h \cdot k_{\mathrm{turn}}
\quad [\mathrm{rad}/\mathrm{s}].
$$

### 2.4 Tracks

Each track is a grid of **tile types** (values 0–6) mapped to PNG assets on the classpath under `/sprites/` (`track_XX.png`). Types represent straights, corners, and open cells. Layouts, names, and terrains are defined per track in `/data/config/tracks.properties` and loaded into `GameConfig.TRACK_MAPS`. Start positions are defined per track in `Circuit.START_POSITIONS`.

The finish line is a geometric segment near the first grid slot; crossing direction determines valid lap increments.

## 3. Software architecture

### 3.1 MVC organization

The codebase follows **Model-View-Controller**:

| Layer | Responsibility | Main classes |
|-------|----------------|--------------|
| **Model** | Game state, rules, persistence | `Car`, `Circuit`, `HallOfFame`, `Result` |
| **View** | Single-window Swing UI | `AppShell`, `GameFrame`, screen panels |
| **Controller** | Loop, input, AI | `Game`, `Controller`, `HumanController`, `AiController`, `GameTickTask`, `Main` |

### 3.2 Observer pattern

`Car` and `Circuit` extend `java.util.Observable` and notify `GameFrame` when state changes. `HallOfFame` notifies `HallPanel` when rankings change. This was a standard Swing-era pattern taught in the course; it is deprecated in modern Java but preserved for historical fidelity until a listener-based refactor is undertaken.

### 3.3 Game loop

`Game` starts a `java.util.Timer` that fires every $\Delta t = 10\,\mathrm{ms}$ (`Game.TICK_INTERVAL_MS`). Each tick (`GameTickTask`):

1. Advances circuit time and notifies the view.
2. Checks whether any car exceeded the lap count (race end).
3. For each controller: enforce track boundaries, update car physics, resolve pairwise collisions.

Human input is handled on the EDT via `KeyListener` on the race `GameFrame` canvas. AI logic runs inside the timer callback.

## 4. Detailed design

### 4.1 Car physics (`Car`)

State includes position $(x, y)$, heading $\theta$, speed $v$, acceleration $a$, and motion state (idle, accelerating, coasting/braking). Simulation uses SI meters; the view converts with $1\,\mathrm{m} \mapsto$ `PIXELS_PER_METER` pixels.

Each step (`applyPhysics`), for step size $\Delta t$:

- Applies friction when no throttle input is held.
- Clamps speed to the model’s maximum $v_{\max}$.
- Integrates pose as a unicycle while steering is held and $v \neq 0$:

$$
\begin{aligned}
\theta &\leftarrow \theta + s\,\omega_{\max}\,\Delta t, \\
x &\leftarrow x + v\cos\theta\,\Delta t, \\
y &\leftarrow y + v\sin\theta\,\Delta t, \\
v &\leftarrow v + a\,\Delta t,
\end{aligned}
$$

where $s \in \{-1,0,+1\}$ is the steer direction. Steering has no effect while stopped.

- Updates lap count via `Circuit.crossFinishLine`.

Collisions (`collideWith`) use axis-aligned rectangles rotated per car, then adjust speeds and headings with a simple elastic blend constant.

### 4.2 Track logic (`Circuit`)

`enforceTrackBoundaries` maps the car to grid coordinates and checks whether the pixel position lies inside the valid corridor for that tile type:

- **Types 0–1:** straight segments with inner/outer radius bounds.
- **Types 2–5:** quarter-circle corners around tile corners.
- **Type 6:** open tile (no asphalt constraint).

Leaving the corridor reverses part of the speed and nudges the car back toward the tile centre.

### 4.3 AI controller (`AiController`)

AI opponents plan with a geometric **reference path** (`TrackGeometry` / `ReferencePath`) using PD and sparse short-horizon **MPCC**, then map the resulting speed / turn desires onto the **same arcade controls** humans use (accelerate, brake, steer left/right). The shared `Car.applyPhysics` plant enforces identical saturations and blocks steering while stopped.

#### PD path follower

On a clear road, `PdPathFollowController` tracks the reference line. With cross-track error $e_{\perp}$, heading error $e_{\theta}$, path curvature $\kappa$, and cruise $v_{\mathrm{cruise}}$:

$$
\begin{aligned}
\alpha_{\perp} &= \operatorname{atan2}\!\big(k_{p,\perp}\,e_{\perp},\;\max(|v|, v_{\min})\big), \\
\omega &= v\,\kappa + k_{p,\theta}\,(e_{\theta} - \alpha_{\perp}) + k_{d,\theta}\,\dot{e}_{\theta}, \\
v_{\mathrm{ref}} &= \frac{v_{\mathrm{cruise}}}{1 + k_{\kappa}\,|\kappa|}, \\
v_{\mathrm{cmd}} &= v + k_{p,v}\,(v_{\mathrm{ref}} - v) + k_{d,v}\,\dot{e}_{v}.
\end{aligned}
$$

#### Sparse Dubins MPCC

When opponents or walls are nearby, `DubinsMpccPlanner` solves a short-horizon shooting problem over commands $(v_k, \omega_k)$ for $k = 0,\ldots,N-1$ (horizon $N$, sample time $\delta t$). The scalar cost rewards progress and soft-penalizes contouring outside a free band, heading error, lag, control rates, actuator saturation, walls, and cars:

$$
\begin{aligned}
J &= \sum_{k=1}^{N}\Big[
  w_{c}\,\tilde{e}_{\perp,k}^{2}
  + w_{\theta}\,e_{\theta,k}^{2}
  + w_{\ell}\,\ell_{k}^{2}
  - w_{p}\,p_{k}
  + w_{v}\,[v_{\mathrm{ref},k} - v_{k}]_{+}^{2} \\
&\qquad
  + w_{u}\,\big(\Delta v_{k}^{2} + \Delta\omega_{k}^{2}\big)
  + J_{\mathrm{act},k}
  + J_{\mathrm{wall},k}
  + J_{\mathrm{car},k}
\Big],
\end{aligned}
$$

where $\tilde{e}_{\perp}$ is cross-track error beyond the contour dead-zone, $\ell$ is lag along the path, $p$ is forward progress, and $[\cdot]_{+}$ is the positive part. Walls use a much larger weight than cars (asphalt first; traffic is soft and risk-tolerant).

All-AI exhibition demos (`view.DemoRaceCapture` / `./gradlew demoRace -PTRACK=… -PCARS=…`) take a track id and car ids (`-PTRACK=3 -PCARS=0,0,0,0`, or `identical` / `identical:N`; spaces also work if quoted).

### 4.4 Hall of Fame (`HallOfFame`, `Result`)

Each track keeps ten best results ranked by **mean lap time**

$$
\bar{t} = \frac{T}{N_{\mathrm{laps}}},
$$

so races with different lap counts stay comparable ($T$ is total duration). `Result` stores player name, total duration in milliseconds, lap count, car model index, and timestamp. Data is serialized to an OS-specific user directory (Linux: `$XDG_DATA_HOME/super-sprint-supelec/hall_of_fame.dat`, default `~/.local/share/super-sprint-supelec/hall_of_fame.dat`), seeded from classpath `/data/hall_of_fame.dat` on first run. On first run failure (or corrupt / incompatible file — `Result` `serialVersionUID` is `3`), default placeholder entries are created.

When a race ends, `Game` asks `AppShell` to show the race-complete screen. Human winners who place may save via `HallOfFame.addResult`; computer wins are shown but never written to the board.

### 4.5 View layer

- **`AppShell`** — the sole application window; navigates between main menu, race setup, Hall of Fame, help, race complete, and race.
- **`GameFrame`** — race canvas with buffer-strategy rendering of track tiles, cars, and HUD.
- **`HallPanel` / `HelpPanel` / `RaceCompletePanel`** — in-window screens swapped by `AppShell`.

All user-visible strings were translated to English during the 2026 refactor; the original UI was French.

## 5. Implementation notes

### 5.1 Technology stack

- **Language:** Java (originally Java 7/8 era; currently validated on **JDK 17+**).
- **UI:** Swing (`JFrame`, `JComboBox`, `JTable`, …).
- **Build (2015):** manual `javac` invocation.
- **Build (current):** **Gradle** (wrapper `./gradlew`) with the standard `src/main` / `src/test` layout; see [BUILD.md](BUILD.md). A thin Makefile only wraps Gradle tasks.
- **CI (current):** GitHub Actions runs `./gradlew classes`, `test`, and `smokeTest`; release tags publish demo videos and platform binaries.

### 5.2 Asset pipeline

Graphics are static PNG files on the classpath under `/sprites/`:

- Car sprites: `cars.png` (3×3 sheet) sliced at build into race `car_00.png` … `car_08.png` and larger menu `car_00_menu.png` … `car_08_menu.png` (soft cyan matte)
- Track tiles: `track_00.png` … `track_06.png` (same naming; wide Super Sprint lane geometry; Kenney-inspired asphalt from `scripts/generate-track-tiles.py`)
- Terrain scenery: Kenney Top-down Tanks Redux grass/sand tiles + green/brown trees (CC0; zip vendored under `third_party/`, extracted to `/sprites/kenney/` at build)
- UI: `icon.png`, splash, generated `track_preview_XX.png`

Tile size in pixels is `GameFrame.TILE_SIZE` (219 px). Physics radii (`INNER_RADIUS` / `OUTER_RADIUS`) stay tied to these wide tiles; narrower third-party road packs are not drop-in replacements. A future art pass could unify background, track, and cars under one sprite set or original designs.

### 5.3 Entry point

```bash
./gradlew run
# equivalent after installDist: build/install/super-sprint-supelec/bin/super-sprint-supelec
```

`Main` constructs `AppShell`, which bootstraps the Hall of Fame and menu UI in one window.

## 6. Testing and validation

### 6.1 Original testing (2015)

Validation was primarily **manual**: play-through on each track, two-player input, Hall of Fame read/write, and AI completion of laps. No automated unit tests were part of the course deliverable.

### 6.2 Current automated checks (2026)

The CI pipeline verifies:

1. **Compilation** - all sources under `src/` compile without errors.
2. **Launch smoke test** - the JVM starts, Swing initializes under a virtual framebuffer, and the process remains alive for several seconds without crashing.

Contributors should run `./gradlew test` and `./gradlew smokeTest` before opening pull requests (see [CONTRIBUTING.md](CONTRIBUTING.md)).

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

Possible extensions: broader automated gameplay tests, replays, additional tracks, sound, and replacing Java serialization with JSON.

## 8. Conclusion

The Super Sprint Supélec project successfully met its academic goals: a playable arcade racer with menus, AI opponents, persistence, and documented object-oriented structure. The MVC split made it possible to evolve rendering and control logic independently of core physics.

The 2026 maintenance work added contributor standards, a Gradle-based reproducible build with classpath resources, CI smoke testing and release packaging, English naming throughout the source tree, and this translated report so the project remains approachable without the original French PDF.

## 9. References

1. Original French PDF project report (2015 evaluation submission - not stored in this repository).
2. Course materials: Supélec *Projet Logiciel*, Sequence 6, 2014/2015.
3. Inspiration: *Super Sprint* (arcade, 1985).
4. Repository documentation: [README.md](../README.md), [BUILD.md](BUILD.md), [CONTRIBUTING.md](CONTRIBUTING.md).

## Appendix A - Class inventory (current packages)

| Class | Package | Role |
|-------|---------|------|
| `Main` | controller | Application entry point |
| `Game` | controller | Race session orchestration |
| `Controller` | controller | Abstract player/AI binding to `Car` |
| `HumanController` | controller | Keyboard input |
| `AiController` | controller | Path-following AI with sparse MPCC avoidance |
| `PdPathFollowController` | controller | PD tracking on a reference path |
| `HybridMpccPathFollowController` | controller | PD default + sparse Dubins MPCC |
| `DubinsMpccPlanner` | controller | Short-horizon shooting MPCC |
| `TrackingLoop` | controller | Dubins vehicle + path-follower step |
| `GameTickTask` | controller | Timer tick callback |
| `Car` | model | Physics and rendering notifications |
| `Circuit` | model | Track grid, boundaries, timing |
| `TrackGeometry` / `ReferencePath` | model | Centerline samples for AI / previews |
| `Terrain` / `GameCatalog` | model | Cars, tracks (from properties), lap options |
| `HallOfFame` / `Result` | model | Leaderboard persistence |
| `AppShell` | view | Single application window |
| `GameFrame` | view | Race canvas rendering |
| `HallPanel` / `HelpPanel` / `RaceCompletePanel` | view | In-window screens |
| `RaceSceneryPainter` | view | Terrain ground fill + flora sprites |

## Appendix B - Glossary (French → English)

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
