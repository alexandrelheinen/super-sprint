# Kenney — Top-down Tanks Redux

Vendored CC0 asset pack used for race scenery (grass/sand ground tiles and
green/brown trees).

- Archive: `kenney_topdownTanksRedux.zip`
- License: `License.txt` (CC0 1.0)
- Upstream: https://opengameart.org/content/top-down-tanks-redux

`./gradlew processResources` runs `scripts/prepare-kenney-sprites.sh`, which
extracts only the grass/sand tiles and green/brown tree sprites onto the
classpath under `/sprites/kenney/` (tanks, bullets, roads, and other pack
files stay in the zip unused).
