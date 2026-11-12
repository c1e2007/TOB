# Theatre of Blood (ToB) for OpenRune-Server

OSRS Theatre of Blood (ToB) raid content implementation for [OpenRune-Server](https://github.com/OpenRune/OpenRune-Server) (revision 240.2 / 241).

---

## 🏛️ Architecture & Overview

This port provides a full modular implementation of Theatre of Blood, engineered specifically for the OpenRune plugin architecture:

- **Static Cache Coordinates**: The ToB arena is real, fully-modeled terrain in the OSRS cache located at `x 3136–3264, z 4224–4480`. Instead of hand-rolling custom instances that cause collision voids, encounters operate directly on these canonical cache coordinates with native collision and pathfinding.
- **Forward-Compatible Instancing Hook**: All coordinate arithmetic flows through `resolve(coord)` and `resolve(session, coord)`. When dynamic instance allocation is activated in the future, the engine passes the session and transforms coordinates transparently without touching room combat logic.
- **Self-Cleaning Encounters**: Each room encounter (`MaidenEncounter`, `BloatEncounter`, etc.) tracks its own spawned NPCs, projectiles, and arena hazards (blood pools, flies, webs, acid, etc.), cleanly despawning them on room transition, wipe, or reset.
- **Party & Raid Management**: In-memory raid session state tracked in `TobRaids`, `TobParties`, and `TobLobby` without rigid session ID dependencies.

---

## 🗺️ Room & Coordinate Mappings

All rooms are mapped to their canonical cache coordinates:

| Room | Boss / Objective | Cache Coordinates `(x, z, level)` | Notes |
|---|---|---|---|
| **The Sugadinti Maiden** | Maiden of Sugadinti | `3162, 4444, 0` | Blood splats & Nylocas spawns |
| **The Pestilent Bloat** | Pestilent Bloat | `3305, 4448, 0` | Sleep cycles, flies, tank paths |
| **The Nylocas** | Nylocas Vasilias | `3295, 4248, 0` | Wave spawns, pillars, boss phase |
| **Sotetseg** | Sotetseg | `3279, 4309, 0` | Red ball attack, maze transition |
| **The Shadow Realm** | Shadow Maze | `3360, 4306, 0` | Chunk `(416, 536)` |
| **Xarpus** | Xarpus | `3170, 4381, 1` | Sits on height level 1 |
| **Lady Verzik Vitur** | Lady Verzik Vitur | `3168, 4302, 0` | Phase 1, Phase 2, Phase 3 |
| **Treasure Vault** | Reward Chests | `3237, 4319, 0` | Spectate `3237, 4328, 0` |

---

## 🚪 Verified Cache Locs

Verified against cache definitions:

- **`33113`** — *Formidable passage* (`[0=Enter, 1=Quick-Enter]`) — Inter-room barrier passage
- **`32751`** — *Door* (`[0=Enter, 1=Quick-enter]`) — Entrance to Verzik Vitur arena
- **`32738`** — *Treasure room* (`[0=Enter]`) — Passage into the Reward Vault
- **`32653`** — *Theatre of Blood* (`[0=Enter]`) — Lobby main raid entrance
- **`32655`** — *Notice board* (`[0=Read]`) — Lobby party recruitment board

---

## 🎮 In-Game Commands

For testing and rapid debugging in-game:

- `::tobstart` — Auto-creates party (if needed), starts raid, and teleports player to Maiden arrival.
- `::tobreset` — Cleans up active hazards, despawns room NPCs, resets room cleared state, and respawns the boss for the active room.
- `::toblobby` / `::tobleave` — Leaves the current raid and returns to the lobby entrance.

### Room Teleports:
- `::tobmaiden` — Teleport to The Sugadinti Maiden
- `::tobbloat` — Teleport to Pestilent Bloat
- `::tobnylo` — Teleport to The Nylocas
- `::tobsotetseg` — Teleport to Sotetseg
- `::tobshadow` — Teleport to Shadow Realm
- `::tobxarpus` — Teleport to Xarpus (level 1)
- `::tobverzik` — Teleport to Verzik Vitur
- `::tobreward` — Teleport to the Reward Vault

---

## 📁 Repository Structure

```
.
├── README.md
├── build.gradle.kts                # OpenRune module build definition
└── tob/                            # Package org.rsmod.content.raids.tob
    ├── party/                      # Party creation, settings, lobby logic
    ├── raid/                       # TobRaid, TobRoom, TobFight, progression
    ├── room/
    │   ├── bloat/                  # Bloat boss, arena, fight, scripts
    │   ├── maiden/                 # Maiden boss, arena, blood pools, fight
    │   ├── nylocas/                # Nylo waves, pillars, boss, fight
    │   ├── reward/                 # Chest interfaces, loot tables, vault
    │   ├── shadowrealm/            # Shadow maze stub & encounter
    │   ├── sotetseg/               # Sotetseg boss, attacks, maze
    │   ├── verzik/                 # Verzik P1/P2/P3 phases, arena
    │   └── xarpus/                 # Xarpus boss, acid pools, stare
    ├── TobBarrierScript.kt         # Barrier pass logic & prompt dialogs
    ├── TobCheatScript.kt           # Developer test commands
    ├── TobCoords.kt                # Forward-compatible coord translation hook
    ├── TobEntryScript.kt           # Lobby board & passage entry interactions
    ├── TobModule.kt                # Guice DI module
    └── TobProgressionScript.kt     # Room transitions & wipe handling
```

---

## 🛠️ Integration with OpenRune-Server

To use this content in OpenRune-Server:

1. Clone or copy into:
   ```
   OpenRune-Server/content/raids/tob/
   ```
2. Ensure the source files sit under:
   ```
   content/raids/tob/src/main/kotlin/org/rsmod/content/raids/tob/
   ```
3. Compile and verify:
   ```bash
   ./gradlew :content:raids:tob:compileKotlin
   ```
4. Run the server:
   ```bash
   ./gradlew run
   ```
