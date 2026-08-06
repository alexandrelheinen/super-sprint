# Super Sprint Supélec - build instructions

This project is a plain Java application (no Maven/Gradle). All build and run commands execute from the **repository root** so relative paths to `src/sprites/` and `src/data/` resolve correctly.

## Prerequisites

- **JDK 17+** with `javac` and `java` on your `PATH`
- **GNU Make**
- **Python 3** with **Pillow** (`python3-pil` or `pip install Pillow`) to slice `src/sprites/cars.png` at build time
- **Xvfb** (optional, only for headless smoke testing on Linux CI or servers)

Check your setup:

```bash
java -version
javac -version
make --version
```

## Quick reference

| Command                 | Description                                      |
|-------------------------|--------------------------------------------------|
| `make compile`          | Compile all sources into `build/`                |
| `make run`              | Compile (if needed) and launch the game          |
| `make smoke-test`       | Headless launch test (exits after a few seconds) |
| `make package`          | Build portable zip (requires JDK 17+ to run)     |
| `make package-appimage` | Build OS app-image with bundled Java runtime     |
| `make clean`            | Remove compiled classes and prepared sprites     |
| `make help`             | List available targets                           |

## Compile

```bash
make compile
```

Sources live under `src/` in packages `controller`, `model`, and `view`. Class files are written to `build/` mirroring the package structure.

During compilation, `scripts/prepare-car-sprites.sh` slices the 3×3 sheet `src/sprites/cars.png` into nine race sprites (`car_00.png` … `car_08.png`) and nine larger menu sprites (`car_00_menu.png` … `car_08_menu.png`). Cyan-ish background pixels are removed with a soft alpha matte (border unmix / spill suppression), empty margins are trimmed, each car is rotated to face right, then scaled to race and menu sizes. Mean-color and size metadata are written to `cars.properties`. The source sheet is kept; derived sprites are written to `build/sprites/` and mirrored under `src/sprites/` as bundled fallbacks.

Equivalent manual command:

```bash
mkdir -p build
find src -name '*.java' > build/sources.txt
javac -d build -sourcepath src @build/sources.txt
```

## Run

```bash
make run
```

Equivalent manual command:

```bash
make compile
java -cp build controller.Main
```

## Headless smoke test

Used by CI to verify the application starts without crashing:

```bash
make smoke-test
```

This wraps the JVM with `xvfb-run` and terminates after five seconds. A exit code of `0` means the process started successfully.

## Release demo videos

Pushing a `v*` tag triggers `.github/workflows/release-demos.yml`. The job runs `make record-demo` twice under Xvfb and attaches the MP4s to the GitHub Release:

| Asset | Command |
|-------|---------|
| `ai-demo-fastest-increasing-dune-horseshoe.mp4` | `make record-demo TRACK=3 CARS=2,1,7,4` |
| `ai-demo-slowest-increasing-desert-elbow.mp4` | `make record-demo TRACK=1 CARS=5,6,3,0` |

Car lists are ascending `maxSpeed` (slowest in front, fastest last): fastest pack `2,1,7,4`, slowest pack `5,6,3,0`.

## Release binaries

Pushing a `v*` tag also triggers `.github/workflows/release-binaries.yml`, which builds downloadable packages and attaches them to the same GitHub Release:

| Asset | What it is |
|-------|------------|
| `SuperSprintSupelec-VERSION-linux-x64.tar.gz` | Linux app-image with a bundled JRE — unpack and run `SuperSprintSupelec/bin/SuperSprintSupelec` |
| `SuperSprintSupelec-VERSION-windows-x64.zip` | Windows app-image with a bundled JRE — unpack and run `SuperSprintSupelec.exe` |
| `SuperSprintSupelec-VERSION-portable.zip` | Cross-platform jar + assets — still needs JDK/JRE 17+; use `run.sh` / `run.bat` |

Build the same artifacts locally:

```bash
make package VERSION=2.1.0            # portable zip only
make package-appimage VERSION=2.1.0   # portable zip + current-OS app-image
```

Artifacts are written under `artifacts/release/`. Packaged launches resolve sprites and config relative to the application home (directory next to the jar, or `-Dsuper.sprint.home=...`).

**Mobile:** not supported. This is a Java Swing desktop game; phone/tablet builds would need a different UI toolkit (for example LibGDX or a native port).

## Clean

```bash
make clean
```

Removes `build/` and generated source lists. Does **not** delete the user Hall of Fame file under `~/.local/share/super-sprint-supelec/`.

## User data

On first launch, the game copies `src/data/hall_of_fame.dat` into an OS-specific user data directory:

| Platform | Location |
|----------|----------|
| Linux | `$XDG_DATA_HOME/super-sprint-supelec/hall_of_fame.dat`, or `~/.local/share/super-sprint-supelec/` when `XDG_DATA_HOME` is unset |
| Windows | `%APPDATA%\super-sprint-supelec\hall_of_fame.dat` |
| macOS | `~/Library/Application Support/super-sprint-supelec/hall_of_fame.dat` |

Subsequent runs read and write leaderboard data from that user file only.

## Troubleshooting

| Problem | Likely cause | Fix |
|---------|--------------|-----|
| `Could not find or load main class controller.Main` | Not compiled or wrong directory | Run `make compile` from repo root |
| Missing sprites / file not found | Wrong working directory | Always run from repository root |
| Car sprite preparation failed | Missing Pillow or `cars.png` | Install Pillow (`pip install Pillow` / `python3-pil`) and ensure `src/sprites/cars.png` exists |
| HeadlessException on CI | No display | Use `make smoke-test` (includes Xvfb) |
| Deprecation warnings for Observer | Legacy observer pattern | Warnings are expected; see CONTRIBUTING.md |

See also [README.md](../README.md) for gameplay controls and [CONTRIBUTING.md](CONTRIBUTING.md) for coding standards.
