# Configuration files

Edit the `.properties` files in this directory before building to customize the game
without changing Java source code.

| File | Purpose |
|------|---------|
| `catalog.properties` | Car and track names, lap options, Hall of Fame seed names |
| `game.properties` | Window title and race rules (max cars, human players) |
| `theme.properties` | UI colors (RGB), glass surfaces, HUD strip, and font family/sizes |
| `ui.properties` | Reference resolution and minimum window/font sizes |
| `messages.properties` | Menu labels, help text, and dialog copy |

Files are loaded in alphabetical order; a key defined in multiple files uses the
last file loaded. Use `\n` in `messages.properties` for line breaks.

Run `make compile` after editing so the build picks up changes.
