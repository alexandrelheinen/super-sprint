# Super Sprint Supélec

<img src="src/sprites/icon.png" alt="Super Sprint" width="120" align="left" style="margin-right: 15px; margin-bottom: 15px;">

This project was carried out as part of the Supélec engineering curriculum (*Projet Logiciel*, Sequence 6) between November 2014 and February 2015.

The goal was to design and implement a complete desktop application in Java, from requirements analysis through to a playable prototype with documentation.

The chosen theme is a simplified clone of the arcade game [Super Sprint](http://www.giantbomb.com/super-sprint/3030-2776/), a top-down car race on modular tracks, with lap counting, collisions, and a persistent leaderboard.

## Features

- Top-down arcade racing with nine car liveries and four track layouts
- One- or two-player local multiplayer (remaining slots filled by AI opponents)
- Configurable lap counts with race timer
- Hall of Fame leaderboard stored in the Linux user data directory
- Path-following PD controller for AI drivers

## Requirements

- JDK 17 or newer
- GNU Make
- Python 3 with Pillow (slices `src/sprites/cars.png` at build time)
- A graphical environment to play (X11 on Linux, native display on macOS/Windows)

## Quick start

From the repository root:

```bash
make run
```

This compiles sources into `build/` and launches `controller.Main`.

Other useful commands:

```bash
make compile      # compile only
make smoke-test   # headless startup check (Linux CI)
make clean        # remove build artifacts
make help         # list targets
```

See [docs/BUILD.md](docs/BUILD.md) for detailed build instructions and troubleshooting.

## Controls

| Player | Accelerate / brake | Turn |
|--------|-------------------|------|
| 1      | ↑ / ↓ arrow keys  | ← / → arrow keys |
| 2      | W / S             | A / D |

Choose the lap count in Race Setup (default **3**). The first car to finish wins.

## Project layout

```
src/
  controller/   Game loop, input, AI (Main entry point)
  model/        Car physics, track logic, Hall of Fame persistence
  view/         Swing menus and race rendering
  sprites/      Bundled PNG assets (cars + menu cars, tracks, UI)
  third_party/  Vendored Kenney Top-down Tanks Redux zip (build extracts scenery)
  data/         Seed Hall of Fame file copied on first run
docs/           Markdown documentation
build/          Compiled classes and prepared car sprites (generated)
```

On Linux, runtime Hall of Fame data is stored at `$XDG_DATA_HOME/super-sprint-supelec/hall_of_fame.dat` (default: `~/.local/share/super-sprint-supelec/hall_of_fame.dat`), initialized from `src/data/hall_of_fame.dat` when missing.

## Architecture

The codebase follows **Model–View–Controller**:

```mermaid
flowchart LR
  AppShell --> Game
  Game --> GameFrame
  Game --> Circuit
  Game --> Controller
  Controller --> Car
  Car --> Circuit
  GameFrame --> Car
  GameFrame --> Circuit
  HallOfFame --> HallPanel
  AppShell --> HallOfFame
```

- **Model** — `Car`, `Circuit`, `HallOfFame`, `Result`, `ResourcePaths`
- **View** — `AppShell` (single window), `GameFrame` (race canvas), screen panels
- **Controller** — `Game`, `Controller`, `HumanController`, `AiController`, `GameTickTask`

See [docs/REPORT.md](docs/REPORT.md) for the full design document (English translation of the original French project report).

## Documentation

| File | Purpose |
|------|---------|
| [README.md](README.md) | Quick start (this file) |
| [docs/BUILD.md](docs/BUILD.md) | Build and run instructions |
| [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) | Code standards for contributors |
| [docs/AGENTS.md](docs/AGENTS.md) | Checklist for AI coding assistants |
| [docs/REPORT.md](docs/REPORT.md) | Project report and architecture |

## Continuous integration

GitHub Actions (`.github/workflows/ci.yml`) compiles the project and runs a headless launch smoke test on every push and pull request to `master`.

## License

This project is released under the [MIT License](LICENSE).

Application icon: [Race](https://www.flaticon.com/free-icon/race_4552572) designed by [Magnific](https://www.flaticon.com/authors/magnific) from [Flaticon](https://www.flaticon.com). See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
