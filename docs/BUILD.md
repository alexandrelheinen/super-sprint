# Super Sprint Supélec - build instructions

This project is a standard **Gradle** Java application (Java 17 bytecode). Sources follow the conventional Maven/Gradle layout; assets load from the **classpath**.

## Prerequisites

- **JDK 17+** (`java` on your `PATH`; the Gradle wrapper downloads Gradle itself)
- **Python 3** with **Pillow** (`python3-pil` or `pip install Pillow`) to slice `src/main/resources/sprites/cars.png` at build time
- **Xvfb** (optional, only for headless smoke testing on Linux CI or servers)

Check your setup:

```bash
java -version
./gradlew -v
```

## Project layout

```
src/main/java/           Java sources (controller, model, view)
src/main/resources/      Classpath resources (sprites, data/config, seed HoF)
src/test/java/           JUnit 5 tests
build/generated/…        Build-time sprites (cars, Kenney, track previews)
```

## Quick reference

| Command | Description |
|---------|-------------|
| `./gradlew classes` | Compile sources and prepare resources |
| `./gradlew run` | Launch the game |
| `./gradlew test` | Run the JUnit 5 suite |
| `./gradlew smokeTest` | Headless launch test (Xvfb, short timeout) |
| `./gradlew demoRace -PTRACK=3 -PCARS=0,0,0,0` | All-AI exhibition race |
| `./gradlew packageRelease -PappVersion=1.2.3` | Portable zip (needs JRE to run) |
| `./gradlew packageRelease -PappVersion=1.2.3 -PappImage=true` | Portable zip + OS app-image |
| `./gradlew clean` | Remove build outputs |

A thin `Makefile` mirrors these targets (`make run`, `make test`, …) for convenience; **Gradle is the source of truth**.

## Compile and run

```bash
./gradlew classes
./gradlew run
```

During the build, Gradle:

1. Slices `cars.png` into race/menu sprites and writes generated `cars.properties`
2. Extracts Kenney scenery sprites onto the classpath under `/sprites/kenney/`
3. Renders track preview PNGs
4. Packages everything into `build/resources/main` (and into the runnable jar)

## Headless smoke test

```bash
./gradlew smokeTest
```

## Release demo videos

Pushing a `v*` tag triggers `.github/workflows/release-demos.yml`. The job records two MP4s and attaches them to the GitHub Release (fastest pack on Dune Horseshoe, slowest pack on Desert Elbow).

## Release binaries

Pushing a `v*` tag also triggers `.github/workflows/release-binaries.yml`:

| Asset | What it is |
|-------|------------|
| `SuperSprintSupelec-VERSION-linux-x64.tar.gz` | Linux app-image with bundled JRE |
| `SuperSprintSupelec-VERSION-windows-x64.zip` | Windows app-image with bundled JRE |
| `SuperSprintSupelec-VERSION-portable.zip` | Runnable jar (needs JDK/JRE 17+) |

Sprites and config are **inside the jar** on the classpath. Local packaging:

```bash
./gradlew packageRelease -PappVersion=2.1.0
./gradlew packageRelease -PappVersion=2.1.0 -PappImage=true
```

**Mobile:** not supported (desktop Swing).

## User data

On first launch, the game copies the classpath seed `/data/hall_of_fame.dat` into an OS-specific user data directory:

| Platform | Location |
|----------|----------|
| Linux | `$XDG_DATA_HOME/super-sprint-supelec/` or `~/.local/share/super-sprint-supelec/` |
| Windows | `%APPDATA%\super-sprint-supelec\` |
| macOS | `~/Library/Application Support/super-sprint-supelec/` |

## Troubleshooting

| Problem | Likely cause | Fix |
|---------|--------------|-----|
| Missing sprite / config resource | Resources not prepared | Run `./gradlew classes` or `./gradlew processResources` |
| Car sprite preparation failed | Missing Pillow or `cars.png` | Install Pillow; ensure `src/main/resources/sprites/cars.png` exists |
| HeadlessException on CI | No display | Use `./gradlew smokeTest` (includes Xvfb) |
| Deprecation warnings for Observer | Legacy observer pattern | Expected; see CONTRIBUTING.md |

See also [README.md](../README.md) and [CONTRIBUTING.md](CONTRIBUTING.md).
