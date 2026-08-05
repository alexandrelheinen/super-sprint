# Super Sprint Supélec — build instructions

This project is a plain Java application (no Maven/Gradle). All build steps run from the **repository root** so relative asset paths (`images/`, `halloffame.dat`) resolve correctly.

## Prerequisites

- **JDK 17+** with `javac` and `java` on your `PATH`
- **GNU Make**
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
| `make clean`       | Remove compiled classes                          |
| `make help`        | List available targets                           |

## Compile

```bash
make compile
```

Sources live under `src/` in packages `controller`, `model`, and `view`. Class files are written to `build/` mirroring the package structure.

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

Removes `build/` and generated source lists. Does **not** delete `halloffame.dat` (runtime leaderboard data).

## Legacy compilation note

Older revisions used a hand-maintained `java-files.txt` source list. That workflow has been removed in favor of automatic source discovery via the Makefile.

## Troubleshooting

| Problem | Likely cause | Fix |
|---------|--------------|-----|
| `Could not find or load main class controller.Main` | Not compiled or wrong directory | Run `make compile` from repo root |
| Missing images / file not found | Wrong working directory | Always run from repository root |
| HeadlessException on CI | No display | Use `make smoke-test` (includes Xvfb) |
| Deprecation warnings for Observer | Legacy observer pattern | Warnings are expected; see CONTRIBUTING.md |

See also [README.md](README.md) for gameplay controls and [CONTRIBUTING.md](CONTRIBUTING.md) for coding standards.
