# TextDisplayShapes

A Minecraft library for rendering geometric shapes using TextDisplay entities. Supports both direct Bukkit entity manipulation and packet-based rendering via EntityLib.

## Features

- **Multiple Shape Types**: Triangle, Line, Polyline, Parallelogram
- **Two Rendering Modes**:
  - **Bukkit Mode**: Direct entity manipulation, visible to all players
  - **Packet Mode**: EntityLib-based, only visible to specified viewers
- **Modular Design**: Platform-agnostic API module can be reused across Bukkit, Fabric, Minestom, etc.
- **Customizable**: Color, brightness, transparency, double-sided rendering
- **Display Control**: Adjustable view range (`viewRange`)
- **Line Roll Angle**: Rotate lines around their length axis for visibility from any angle

## Module Structure

```
TextDisplayShapes/
├── api/       Platform-agnostic interfaces & math utilities (JOML only)
├── bukkit/    Bukkit implementation using direct entity manipulation
└── packet/    Packet-based implementation using EntityLib/PacketEvents
```

| Module | Artifact ID | Description |
|--------|-------------|-------------|
| **API** | `textdisplayshape-api` | Core interfaces (`Shape`, `ShapeBuilder`) and math utilities. No platform dependencies. |
| **Bukkit** | `textdisplayshape-bukkit` | Bukkit/Paper implementation using real TextDisplay entities. |
| **Packet** | `textdisplayshape-packet` | Packet-based implementation using EntityLib + PacketEvents. |

## Installation

### Maven

```xml
<repository>
    <id>twme-repo-snapshots</id>
    <name>TWME Repository</name>
    <url>https://repo.twme.dev/snapshots</url>
</repository>
```

**Bukkit mode only:**

```xml
<dependency>
    <groupId>dev.twme</groupId>
    <artifactId>textdisplayshape-bukkit</artifactId>
    <version>2.0.0</version>
</dependency>
```

**Packet mode only:**

```xml
<dependency>
    <groupId>dev.twme</groupId>
    <artifactId>textdisplayshape-packet</artifactId>
    <version>2.0.0</version>
</dependency>
```

**API only (for custom platform implementations):**

```xml
<dependency>
    <groupId>dev.twme</groupId>
    <artifactId>textdisplayshape-api</artifactId>
    <version>2.0.0</version>
</dependency>
```

## Usage

> **Note**: When providing a `Location` as the origin, please avoid including the pitch and yaw (set them to 0). Passing a location with rotation data (e.g., `player.getLocation()`) may cause unexpected behavior in shape orientation.

### Bukkit Mode (Direct Entity)

```java
BukkitShapeFactory bukkit = new BukkitShapeFactory();

// Triangle
Shape triangle = bukkit.triangle(spawnLocation, p1, p2, p3)
    .color(Color.fromARGB(150, 50, 100, 100))
    .doubleSided(true)
    .build();
triangle.spawn();

// Line
Shape line = bukkit.line(spawnLocation, p1, p2, 0.1f)
    .rollDegrees(90f)  // Rotate for visibility from different angles
    .viewRange(1.0f)
    .doubleSided(true)
    .build();
line.spawn();

// Polyline (connected line segments)
List<Vector3f> points = Arrays.asList(
    new Vector3f(0, 0, 0),
    new Vector3f(5, 0, 0),
    new Vector3f(5, 5, 0),
    new Vector3f(0, 5, 0)
);
Shape polyline = bukkit.polyline(spawnLocation, points, 0.1f)
    .closed(true)  // Connect last point to first
    .build();
polyline.spawn();

// Parallelogram
Shape parallelogram = bukkit.parallelogram(spawnLocation, p1, p2, p3)
    .build();
parallelogram.spawn();

// Remove shape
triangle.remove();
```

### Packet Mode (EntityLib)

Requires [EntityLib](https://github.com/Tofaa2/EntityLib) and [PacketEvents](https://github.com/retrooper/packetevents).

```java
// Initialize EntityLib first
SpigotEntityLibPlatform platform = new SpigotEntityLibPlatform(plugin);
APIConfig settings = new APIConfig(PacketEvents.getAPI())
    .tickTickables()
    .trackPlatformEntities();
EntityLib.init(platform, settings);

PacketShapeFactory packet = new PacketShapeFactory();

// Create shape
Shape line = packet.line(spawnLocation, p1, p2, 0.1f)
    .color(Color.RED)
    .viewRange(1.0f)
    .build();

// Add viewers by UUID (only these players will see the shape)
line.addViewer(player.getUniqueId());
line.spawn();

// Remove viewer
line.removeViewer(player.getUniqueId());

// Remove shape
line.remove();
```

## API Reference

### Shape Interface

| Method | Description |
|--------|-------------|
| `spawn()` | Spawn the shape into the world |
| `remove()` | Remove the shape from the world |
| `isSpawned()` | Check if the shape is spawned |
| `addViewer(UUID)` | Add a viewer by UUID (packet mode only) |
| `removeViewer(UUID)` | Remove a viewer by UUID (packet mode only) |
| `getViewerUUIDs()` | Get UUIDs of all viewers |
| `getEntityUUIDs()` | Get UUIDs of all entities in this shape |
| `teleportOrigin(double x, double y, double z)` | Teleport origin to prevent view-range issues |

### Builder Options

| Method | Description |
|--------|-------------|
| `.color(int argb)` | Set background color as ARGB integer |
| `.color(Color)` | Set background color using Bukkit Color (Bukkit/Packet builders) |
| `.doubleSided(boolean)` | Enable double-sided rendering |
| `.brightness(int block, int sky)` | Set brightness (0-15) |
| `.seeThrough(boolean)` | Make visible through blocks |
| `.viewRange(float)` | Set entity view range |
| `.roll(float)` | Line roll angle in radians |
| `.rollDegrees(float)` | Line roll angle in degrees |
| `.closed(boolean)` | Close polyline (connect last to first) |
| `.rootAnchor(boolean)` | Enable root-anchor mode (packet mode only) |

## Migration from 1.x

The 2.0 release includes breaking API changes:

| 1.x | 2.0 |
|-----|-----|
| `TextDisplayShapes.bukkit().line(...)` | `new BukkitShapeFactory().line(...)` |
| `TextDisplayShapes.packet().line(...)` | `new PacketShapeFactory().line(...)` |
| `shape.addViewer(Player)` | `shape.addViewer(player.getUniqueId())` |
| `shape.removeViewer(Player)` | `shape.removeViewer(player.getUniqueId())` |
| `shape.getViewers()` | `shape.getViewerUUIDs()` |
| `shape.teleportOrigin(Location)` | `shape.teleportOrigin(x, y, z)` |
| `.color(Color)` (API) | `.color(int argb)` (API), `.color(Color)` still available in Bukkit/Packet builders |
| Single `TextDisplayShape` artifact | Separate `textdisplayshape-api`, `textdisplayshape-bukkit`, `textdisplayshape-packet` |

> **Why UUID instead of Player?** Bukkit invalidates `Player` objects on respawn, creating new instances with different hash codes. Storing `Player` references in a `Set` causes memory leaks and silent removal failures. Using `UUID` avoids these issues entirely.

## JavaDoc

You can find the JavaDoc [here](https://repo.twme.dev/javadoc/snapshots/dev/twme/TextDisplayShape/2.0.0).

## Dependencies

- Spigot/Paper 1.21+
- [PacketEvents](https://github.com/retrooper/packetevents) (for packet mode)
- [EntityLib](https://github.com/Tofaa2/EntityLib) (for packet mode)

## Example Projects
- [WorldEditDisplay](https://github.com/TWME-TW/WorldEditDisplay): Renders WorldEdit selections using TextDisplayShapes

## License

Apache License Version 2.0
