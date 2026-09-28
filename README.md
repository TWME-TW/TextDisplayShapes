# TextDisplayShapes

TextDisplayShapes renders geometric shapes with Minecraft Text Display entities. It provides direct Bukkit rendering for normal server entities and packet-only rendering through [VirtualEntities](https://github.com/twme-ai/VirtualEntities) for per-viewer shapes.

Version 3.0.0 replaces the packet module's EntityLib implementation with VirtualEntities and PacketEvents.
Version 3.0.1 adds Minecraft 26.3 support and corrects Text Display line and face orientation.

## Modules

| Module | Artifact | Purpose |
| --- | --- | --- |
| API | `textdisplayshape-api` | Platform-neutral shape interfaces and JOML math utilities. |
| Paper | `textdisplayshape-paper` | Direct Paper entity rendering. |
| Spigot | `textdisplayshape-spigot` | Direct Spigot-compatible entity rendering. |
| Packet | `textdisplayshape-packet` | Per-viewer, packet-only rendering with VirtualEntities. |

## Requirements

- Minecraft 26.3 is the compiled and verified target. The modules keep Java 17 bytecode and their API usage is unchanged by this upgrade, so the existing Text Display floor (Minecraft 1.19.4+) is unaffected.
- Java 17+ at runtime: the published modules keep Java 17 bytecode
- Java 25 to build from source, because the 26.3 APIs are Java 25 class files
- PacketEvents 2.14.0+ initialized by the host platform when using the packet module
- VirtualEntities v0.9.0 is pulled transitively by the packet module

| Component | Version |
| --- | --- |
| Minecraft | 26.3 |
| Paper API | `26.3.build.41-alpha` |
| Spigot API | `26.3-R0.1-SNAPSHOT` |
| PacketEvents | 2.14.0 |
| Build JDK | 25 |
| Runtime bytecode | Java 17 |

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
    <version>3.0.1</version>
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
    <version>v3.0.1</version>
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

## Animation and updates

Spawned shapes can change after they appear. Updates reuse the existing Text Display entities, so the client animates them instead of respawning anything:

```java
PacketLine line = shapes.line(origin, start, end, 0.05f)
        .interpolationDuration(4)   // geometry and color changes animate over 4 ticks
        .teleportDuration(2)        // translate() animates over 2 ticks
        .build();
line.addViewer(player.getUniqueId());
line.spawn();

line.setPoints(newStart, newEnd);   // stretch or rotate the line in place
line.setColor(0x8000FF00);          // fade to translucent green
line.translate(0, 1, 0);            // move the whole shape one block up
```

- `setPoints(...)` is available on `LineShape`, `PolylineShape`, `TriangleShape`, and `ParallelogramShape`. In packet mode, every part of a shape is updated in one VirtualEntities bundle, so it changes in a single frame.
- A polyline whose point count grows animates the existing segments; new segments appear immediately and removed ones disappear.
- Text Display background color follows the same interpolation timing as the transformation.
- `translate` moves the geometry, unlike `teleportOrigin`, which only rebases the entities. Root-anchored packet shapes move by teleporting only their anchor.
- Invalid geometry, such as a zero-length line, is rejected before anything is sent or stored.

### Styles, groups, and boxes

`ShapeStyle` bundles appearance and animation settings and applies them to any builder with `style(...)`. `ShapeGroup` manages several shapes as one: lifecycle, viewers, color, animation settings, and movement. `BoxOutline` (12 edges) and `BoxFaces` (6 outward-facing faces) are groups whose `setBounds` moves every part in place, which makes animated selection boxes and cursors cheap:

```java
ShapeStyle style = ShapeStyle.DEFAULT.withColor(0xC0FFFFFF).withInterpolationDuration(2);
BoxOutline cursor = shapes.boxOutline(origin, min, max, 0.02f, style);
cursor.addViewer(player.getUniqueId());
cursor.spawn();

cursor.setBounds(newMin, newMax);    // glides to the next cell
shapes.batch(() -> {                 // packet mode: several shapes in one frame
    cursor.setColor(0xC0FF4040);
    preview.setPoints(a, b, c);
});
```

Direct Paper and Spigot shapes support the same updates through the Bukkit Display API. Their updates must run on the thread that owns the entities.

`ShapeGeometry` and `BoxGeometry` expose the platform-neutral transforms and box edges and faces for custom renderers.

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
| `translate(dx, dy, dz)` | Move the rendered geometry, animated by the teleport duration. |
| `setColor(argb)` / `getColor()` | Change the background color, animated by the interpolation duration. |
| `setInterpolationDuration(ticks)` | Animate later geometry and color updates. |
| `setTeleportDuration(ticks)` | Animate later `translate` calls (0-59 ticks). |
| `getEntityCount()` | Count the Text Display entities in use. |

Builders provide color, brightness, see-through, view range, double-sided, root-anchor, interpolation duration, teleport duration, style, and line roll controls. The supported shape types are Line, Polyline, Triangle, and Parallelogram, plus the `BoxOutline` and `BoxFaces` groups.

## Migrating From 2.x

- Remove EntityLib initialization and dependencies.
- Use PacketEvents 2.14.0+ and let VirtualEntities manage packet-only entities.
- Prefer `new PacketShapeFactory(VirtualEntities.create())` at plugin scope; the no-argument constructor remains available for small standalone uses and closes its manager when the factory is closed.
- `PacketLine`, `PacketPolyline`, `PacketTriangle`, and `PacketParallelogram` now expose `List<VirtualEntity>` from `getEntities()` instead of EntityLib wrappers.

## Verification

`integration/mineflayer/run-e2e.sh` launches Paper 1.21.11 with PacketEvents, creates a packet-only root-anchored line, and verifies from Mineflayer that Text Display spawn, metadata rebase, root movement, and packet bundle ordering all work. It also checks that an animated `setPoints` and `setColor` update reuses the same entity with the requested interpolation, and that `translate` moves the root anchor with the requested teleport duration. The fixture is compiled against the version installed from the current checkout. The same test can run from the manual GitHub Actions E2E workflow. Mineflayer still only speaks up to Minecraft 26.1, so that harness keeps its 1.21.11 server even though the modules target 26.3.

## Credits

- [TWME-TW/TextDisplayShapes](https://github.com/TWME-TW/TextDisplayShapes), the home of this project.
- [VirtualEntities](https://github.com/twme-ai/VirtualEntities), the virtual entity lifecycle and metadata library used by packet mode.
- [PacketEvents](https://github.com/retrooper/packetevents), packet transport and protocol abstractions.
- [EntityLib](https://github.com/Tofaa2/EntityLib), the previous packet implementation and migration reference.
- [JOML](https://github.com/JOML-CI/JOML), shape transformation math.

## License

Apache License 2.0.
