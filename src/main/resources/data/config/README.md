# Configuration files

Edit the `.properties` files in this directory before building to customize the game
without changing Java source code. They are shipped on the classpath as
`/data/config/*.properties`.

| File | Purpose |
|------|---------|
| `cars.properties` | Per-car index, racing number, name, mean color, race/menu sprite sizes, and stats (regenerated from `cars.png` at build time into `build/generated/…`) |
| `tracks.properties` | Per-track index, name, terrain (`grass`/`sand`), and tile map rows |
| `catalog.properties` | Lap options and Hall of Fame seed names |
| `game.properties` | Window title and race rules (max cars, human players) |
| `theme.properties` | UI colors (RGB), glass surfaces, HUD strip, and font family/sizes |
| `ui.properties` | Reference resolution and minimum window/font sizes |
| `world.properties` | Pixels-per-meter and meters-per-tile world scale |
| `messages.properties` | Menu labels, help text, and dialog copy |

`ConfigLoader` merges these files in alphabetical order; a key defined in multiple
files uses the last file loaded. Generated `cars.properties` overrides the
hand-authored copy when both are present. Use `\n` in `messages.properties` for
line breaks.

Run `./gradlew classes` (or `./gradlew processResources`) after editing so the
build picks up changes.
