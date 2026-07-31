# Argentum
Argentum is a client performance mod for Ornithe 1.8.9, based on the [Celeritas](https://git.taumc.org/embeddedt/celeritas) rendering engine.

## Features
A non-exhaustive list of features that currently exist:

- Rewritten terrain meshing from [Celeritas](https://git.taumc.org/embeddedt/celeritas)
- Entity rendering improvements, including instancing for players, mobs, and animals (and attachments, like armor)
- An optimized font renderer
- An optimized cloud renderer
- Entity and particle occlusion culling
- A Celeritas-based video settings menu

Along with that, some other optimizations are planned:

- Binary greedy meshing
- Block entity instancing (for chests, signs, etc)
- Block entity per-section frustum fast paths
- Cache item frame geometry
- Particle instancing
- Batching HUD draw calls
- Caching HUD elements to reduce draw calls
- ...and more!

A companion mod also exists under the [`extras`](/extras) folder, providing extra rendering customization and eye candy.

The [`cera`](/cera) subproject reimplements MCPatcher/OptiFine resource pack extensions.

## License
Being based on Celeritas, Argentum is licensed under the [version 3.0 of the LGPL](https://www.gnu.org/licenses/lgpl-3.0.html).
