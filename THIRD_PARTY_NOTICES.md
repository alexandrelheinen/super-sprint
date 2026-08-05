# Third-party notices

## Kenney — Top-down Tanks Redux

- Source: [Top-down Tanks Redux](https://opengameart.org/content/top-down-tanks-redux) (Kenney.nl)
- Vendored archive: `third_party/kenney-top-down-tanks-redux/kenney_topdownTanksRedux.zip`
- License file: `third_party/kenney-top-down-tanks-redux/License.txt`
- License: [Creative Commons Zero (CC0 1.0)](https://creativecommons.org/publicdomain/zero/1.0/)
- Credit (optional): Kenney.nl

Build extracts only the scenery sprites we use into `build/sprites/kenney/`
via `scripts/prepare-kenney-sprites.sh` (not the full tanks/roads pack):

- Ground fill tiles: `tileGrass1/2.png`, `tileSand1/2.png`
- Flora: `treeGreen_*` / `treeBrown_*` (large, small, twigs, leaf)

Track tiles (`track_00.png` … `track_06.png`) keep Super Sprint lane geometry and are
**original generated art** inspired by Kenney’s asphalt look
(`scripts/generate-track-tiles.py`). Kenney’s own road tiles are narrower and are
not used as drop-in replacements.
