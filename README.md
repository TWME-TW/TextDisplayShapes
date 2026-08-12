# TextDisplayShapes

TextDisplayShapes renders geometric shapes with Minecraft Text Display entities. It provides direct Bukkit rendering for normal server entities and packet-only rendering through [VirtualEntities](https://github.com/twme-ai/VirtualEntities) for per-viewer shapes.

Version 3.0.0 replaces the packet module's EntityLib implementation with VirtualEntities and PacketEvents.

## Modules

| Module | Artifact | Purpose |
| --- | --- | --- |
| API | `textdisplayshape-api` | Platform-neutral shape interfaces and JOML math utilities. |
| Paper | `textdisplayshape-paper` | Direct Paper entity rendering. |
| Spigot | `textdisplayshape-spigot` | Direct Spigot-compatible entity rendering. |
| Packet | `textdisplayshape-packet` | Per-viewer, packet-only rendering with VirtualEntities. |

## Requirements

- Java 17+
- Minecraft 1.19.4+ for Text Display entities
- PacketEvents 2.13.0+ initialized by the host platform when using the packet module
- VirtualEntities v0.9.0 is pulled transitively by the packet module

## Installation

### Official TWME Maven repository (recommended)

Add the TWME releases repository:

```xml
<repository>
    <id>twme-releases</id>
    <url>https://repo.twme.dev/releases</url>
</repository>
```

Then add the needed module. For packet-only rendering:

```xml
<dependency>
    <groupId>dev.twme</groupId>
    <artifactId>textdisplayshape-packet</artifactId>
    <version>3.0.0</version>
</dependency>
```

The same group and version apply to `textdisplayshape-api`, `textdisplayshape-paper`, and `textdisplayshape-spigot`.

### JitPack fallback

Tagged releases are also available from JitPack as a fallback. JitPack rebuilds
the selected Git tag, while the TWME repository serves the artifacts produced by
the project's release workflow. Prefer the TWME repository for production builds.

```xml
<repository>
    <id>jitpack</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.TWME-TW.TextDisplayShapes</groupId>
    <artifactId>textdisplayshape-packet</artifactId>
    <version>v3.0.0</version>
</dependency>
```

The same JitPack group and tag apply to `textdisplayshape-api`,
`textdisplayshape-paper`, and `textdisplayshape-spigot`.

## Usage

### Direct Paper or Spigot rendering

```java
BukkitShapeFactory shapes = new BukkitShapeFactory();

Shape line = shapes.line(origin, first, second, 0.1f)
        .color(Color.fromARGB(150, 50, 100, 100))
        .doubleSided(true)
        .build();

line.spawn();
line.remove();
```

### Packet-only rendering

Create one application-owned `VirtualEntityManager` and share it with packet shape factories. This gives all shapes a single lifecycle and lets root-anchor relocations use VirtualEntities packet bundles.

```java
VirtualEntityManager entities = VirtualEntities.create();
PacketShapeFactory shapes = new PacketShapeFactory(entities);

Shape line = shapes.line(origin, first, second, 0.1f)
        .color(Color.RED)
        .viewRange(1.0f)
        .rootAnchor(true)
        .build();

line.addViewer(player.getUniqueId());
line.spawn();

// During plugin shutdown.
line.remove();
entities.close();
```

The packet module never creates server-side entities. It resolves viewer UUIDs to online PacketEvents users when a shape is spawned or a viewer is added.

`teleportOrigin(x, y, z)` preserves a shape's world-space geometry by shifting Text Display translation metadata while relocating its virtual origin. For root-anchored shapes, the metadata updates and root teleport are sent in one VirtualEntities bundle on bundle-capable clients, avoiding an intermediate visual jump.

## Shape API

All shape implementations support:

| Method | Purpose |
| --- | --- |
| `spawn()` | Create the shape. |
| `remove()` | Destroy the shape and its virtual entities. |
| `isSpawned()` | Check lifecycle state. |
| `addViewer(UUID)` / `removeViewer(UUID)` | Change packet-mode visibility. |
| `getViewerUUIDs()` | Return a copy of configured viewers. |
| `getEntityUUIDs()` | Return Text Display UUIDs for the shape. |
| `teleportOrigin(x, y, z)` | Rebase the virtual origin without moving the rendered geometry. |

Builders provide color, brightness, see-through, view range, double-sided, root-anchor, and line roll controls. The supported shape types are Line, Polyline, Triangle, and Parallelogram.

## Migrating From 2.x

- Remove EntityLib initialization and dependencies.
- Use PacketEvents 2.13.0+ and let VirtualEntities manage packet-only entities.
- Prefer `new PacketShapeFactory(VirtualEntities.create())` at plugin scope; the no-argument constructor remains available for small standalone uses and closes its manager when the factory is closed.
- `PacketLine`, `PacketPolyline`, `PacketTriangle`, and `PacketParallelogram` now expose `List<VirtualEntity>` from `getEntities()` instead of EntityLib wrappers.

## Verification

`integration/mineflayer/run-e2e.sh` launches Paper 1.21.11 with PacketEvents, creates a packet-only root-anchored line, and verifies from Mineflayer that Text Display spawn, metadata rebase, root movement, and packet bundle ordering all work. The same test can run from the manual GitHub Actions E2E workflow.

## Credits

- [TWME-TW/TextDisplayShapes](https://github.com/TWME-TW/TextDisplayShapes), the home of this project.
- [VirtualEntities](https://github.com/twme-ai/VirtualEntities), the virtual entity lifecycle and metadata library used by packet mode.
- [PacketEvents](https://github.com/retrooper/packetevents), packet transport and protocol abstractions.
- [EntityLib](https://github.com/Tofaa2/EntityLib), the previous packet implementation and migration reference.
- [JOML](https://github.com/JOML-CI/JOML), shape transformation math.

## License

Apache License 2.0.
