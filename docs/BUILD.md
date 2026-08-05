# Super Sprint Supélec — build instructions

This project is a plain Java application (no Maven/Gradle). All build and run commands execute from the **repository root** so relative paths to `src/sprites/` and `src/data/` resolve correctly.

## Prerequisites

- **JDK 17+** with `javac` and `java` on your `PATH`
- **GNU Make**
- **curl** and **ffmpeg** (download and prepare car sprites at build time; bundled sprites are used if preparation fails)
- **Xvfb** (optional, only for headless smoke testing on Linux CI or servers)

Check your setup:

```bash
java -version
javac -version
make --version
```

## Quick reference

| Command            | Description                                      |
|--------------------|--------------------------------------------------|
| `make compile`     | Compile all sources into `build/`                |
| `make run`         | Compile (if needed) and launch the game          |
| `make smoke-test`  | Headless launch test (exits after a few seconds) |
| `make clean`       | Remove compiled classes and prepared sprites     |
| `make help`        | List available targets                           |

## Compile

```bash
make compile
```

Sources live under `src/` in packages `controller`, `model`, and `view`. Class files are written to `build/` mirroring the package structure.

During compilation, `scripts/prepare-car-sprites.sh` downloads GTA 2 car artwork (A-Type, B-Type, Z-Type, T-Rex) from the GTA Wiki, horizontally flips each image so it faces right in-game, and writes PNGs to `build/sprites/car_XX.png` (zero-based, two-digit). If a download or conversion fails, a warning is printed and the bundled `src/sprites/car_XX.png` fallback is copied instead.

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

## Clean

```bash
make clean
```

Removes `build/` and generated source lists. Does **not** delete the user Hall of Fame file under `~/.local/share/super-sprint-supelec/`.

## User data (Linux)

On first launch, the game copies `src/data/hall_of_fame.dat` to:

- `$XDG_DATA_HOME/super-sprint-supelec/hall_of_fame.dat`, or
- `~/.local/share/super-sprint-supelec/hall_of_fame.dat` when `XDG_DATA_HOME` is unset.

Subsequent runs read and write leaderboard data from that user file only.

## Legacy compilation note

Older revisions used a hand-maintained `java-files.txt` source list and stored assets under `images/` at the repository root. That layout has been replaced by `src/sprites/`, `src/data/`, and automatic source discovery via the Makefile.

## Troubleshooting

| Problem | Likely cause | Fix |
|---------|--------------|-----|
| `Could not find or load main class controller.Main` | Not compiled or wrong directory | Run `make compile` from repo root |
| Missing sprites / file not found | Wrong working directory | Always run from repository root |
| Car sprite download failed | Network blocked or missing ffmpeg | Warnings appear during `make compile`; bundled `src/sprites/car*.png` are copied to `build/sprites/` |
| HeadlessException on CI | No display | Use `make smoke-test` (includes Xvfb) |
| Deprecation warnings for Observer | Legacy observer pattern | Warnings are expected; see CONTRIBUTING.md |

See also [README.md](../README.md) for gameplay controls and [CONTRIBUTING.md](CONTRIBUTING.md) for coding standards.
