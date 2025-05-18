# GameProject - Bunny Business

A multiplayer 3D farming simulation game developed as a final project for CSC 165 at Sacramento State. Players farm by day, fend off roaming animals and bee attacks, and interact in real-time through networking.

## Overview
**Genre:** Multiplayer Simulation  
**Theme:** Farming Adventure  
**Dimensionality:** 3D with multi-viewport support

Players start with 10 coins and grow their farm by buying seeds, planting crops, harvesting, and selling produce. Meanwhile, NPC animals (pigs, chickens, and bees) roam the map, creating environmental hazards.

## Gameplay Highlights
- Plant, water, and harvest crops (wheat and carrots)
- Use tools like a watering can and torch
- Buy/sell items at the market with interactive HUD
- Avoid roaming animals and hostile bee attacks
- Interact with dynamic lighting and animated NPCs

## Screenshots
**(1)** Player selects their rabbit color; selection is broadcast to all ghost avatars.

**(2)** In-game view with NPC animals roaming. Bee chases when nearby. HUD shows objectives, status, inventory, and coin count.

**(3)** A grown plant (wheat) after watering. Additional buildings visible.


## Keyboard & Gamepad Controls

### Avatar Controls
| Input | Action |
|-------|--------|
| Keyboard: W / Gamepad: Left Stick Up | Move the rabbit forward |
| Keyboard: S / Gamepad: Left Stick Down | Move the rabbit backward |
| Keyboard: A / Gamepad: Left Stick Left | Turn (yaw) the rabbit left |
| Keyboard: D / Gamepad: Left Stick Right | Turn (yaw) the rabbit right |
| Keyboard: Q | Toggle X,Y,Z axis lines on & off |
| Keyboard: 1 / Gamepad: Button Z (7) | Equip watering can |
| Keyboard: 2 / Gamepad: Button Z (7) | Equip torch |
| Keyboard: SPACE / Gamepad: Button Y (3) | Use selected tool or select option |
| Keyboard: E / Gamepad: Button X (0) | Plant a seed |
| Keyboard: H / Gamepad: Button B (2) | Harvest nearest ready crop |
| Keyboard: M / Gamepad: Button A (1) | Open/interact with market or buildings |
| Keyboard: ESC | Quit the game |

### Camera Controls
| Input | Action |
|-------|--------|
| Keyboard: Arrow UP | MAIN Camera viewport: Move camera up (orbit elevation) |
| Keyboard: Arrow DOWN | MAIN Camera viewport: Move camera down (orbit elevation) |
| Keyboard: Arrow LEFT | MAIN Camera viewport: Rotate orbit camera left (azimuth) |
| Keyboard: Arrow RIGHT | MAIN Camera viewport: Rotate orbit camera right (azimuth) |
| Keyboard: I / Gamepad: D-Pad UP | Mini Camera: Pan overhead camera up |
| Keyboard: J / Gamepad: D-Pad LEFT | Mini Camera: Pan overhead camera left |
| Keyboard: K / Gamepad: D-Pad DOWN | Mini Camera: Pan overhead camera down |
| Keyboard: L / Gamepad: D-Pad RIGHT | Mini Camera: Pan overhead camera right |
| Keyboard: P / Gamepad: RY axis | Mini Camera: Reset viewport offsets |
| Keyboard: U / Gamepad: C-Stick RIGHT | Mini Camera: Zoom in |
| Keyboard: O / Gamepad: C-Stick LEFT | Mini Camera: Zoom out |


## How to Compile & Run
1. `clearTAGEclassfiles`
2. `buildTAGE`
3. Compile:
```bash
javac -Xlint:unchecked a3/*.java
```
4. Start Server:
```bash
javac -cp .;tage.jar tage/networking/server/NetworkingServer.java
java -cp .;tage.jar tage.networking.server.NetworkingServer 6028 UDP
```
5. Run Client:
```bash
java --add-exports java.base/java.lang=ALL-UNNAMED \
     --add-exports java.desktop/sun.awt=ALL-UNNAMED \
     --add-exports java.desktop/sun.java2d=ALL-UNNAMED \
     -Dsun.java2d.d3d=false -Dsun.java2d.uiScale=1 \
     a3.MyGame [insert IP address] 6028 UDP
```

Batch files were created to simplify startup.

## How to Play
- **Buy seeds:** Press `M` near the market → choose "Buy"
- **Plant seed:** Press `E`
- **Water plants:** Use tool `1`, then press `SPACE`
- **Harvest crop:** Press `H` when within range
- **Sell produce:** Press `M` near market → "Sell"

**Crop Timers:** Wheat = 30 sec, Carrot = 45 sec (watering reduces time)

## NPC & Collision System
- **Chickens & pigs:** Push the avatar on contact
- **Bee NPC:** Orbit tree, chases and knocks back/stuns player
- **Environment collisions:** Players/NPCs can't walk through objects

## Scoring System
- Start: 10 coins
- Seeds: 2 coins
- Sell Wheat: 5 coins
- Sell Carrot: 10 coins
- Sell unused seeds: 1 coin
- **Score = coin balance**

## Lighting System
- **Global Ambient Light:** Base lighting across the map
- **Chase Light:** Red light appears when bee chases
- **Market Lamps:** Two static warm lights
- **Torch Light:** Flickering light that follows avatar
- **Crop Spotlights:** Appear on crop maturity, removed on harvest

## Network Protocol Summary
**Server:** `GameServerUDP.java`  
**Client:** `ProtocolClient.java`

**Message Types:**
- `join`, `create`, `move`, `rotate`, `bye`
- `skybox`, `water`, `torch`, `plant`, `harvest`, `grow`, `beeAttack`

**AI Server:** `GameAIServerUDP` handles bee detection and attack messages.

## Genre & Activities
- Farming lifecycle (plant, water, harvest)
- Use tools (watering can, torch)
- Trade at market
- Avoid hostile mobs
- Dynamic terrain + skybox changes every 50s

## Project Features Checklist
### External Models
- `rabbit.obj`, `plant.rkm`, `carrot.obj`, `wheat.obj`, `home.obj`, `market.obj`, `tree.obj`, `torch.obj`, `wateringcan.obj`, `lamp.obj`

### Networking Multiplayer
- Real-time avatar syncing
- Ghost avatars w/ chosen textures
- Shared crop planting/harvesting
- Synced tool visuals & skyboxes

> *Note: Coin count, NPC spawns, and attacks are local only.*

### Skybox & Terrain
- 8 rotating skyboxes every 50 sec
- Last skybox stalls until reset via home
- Terrain textured w/ `hills.jpg`

### Sound
- 3D sound: bee buzz, animal noises, tool sounds
- Background music toggle from radio

### HUD
- Objectives, inventory, status, coin count
- Menus accessible via `M`

### Hierarchical Scene Graph
- Tools parented to avatar
- Bee orbits tree

### Animations
- Bee: looping fly animation
- Chicken & Pig: idle/walk keyframe cycles
- Plant: subtle ruffling

### Physics
- Droplet physics on watering
- Bee & animal knockback on collision
- Environmental collision boundaries

## Asset Attribution
- **Models/Textures/Animations:** Created by team using Blender & Terragen
- **Sounds:**
  - `backgroundMusic.wav` — [Creative Commons via Freesound](https://freesound.org/people/josefpres/sounds/659017/)
  - `beebuzz.wav`, `chickennoise.wav`, `fireSound.wav`, `pigoink.wav`, `water.wav` — [Pixabay License](https://pixabay.com/service/license-summary/)

## Additional Materials
See `Readme.pdf` for further visuals, breakdowns, and development notes.
