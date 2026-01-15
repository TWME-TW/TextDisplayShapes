# TextDisplayShapes

A Minecraft library for rendering geometric shapes using TextDisplay entities. Supports both direct Bukkit entity manipulation and packet-based rendering via EntityLib.

## Features

- **Multiple Shape Types**: Triangle, Line, Polyline, Parallelogram
- **Two Rendering Modes**:
  - **Bukkit Mode**: Direct entity manipulation, visible to all players
  - **Packet Mode**: EntityLib-based, only visible to specified viewers
- **Customizable**: Color, brightness, transparency, double-sided rendering
- **Line Roll Angle**: Rotate lines around their length axis for visibility from any angle

## Installation

### Maven

```xml
<repository>
    <id>twme-repo-snapshots</id>
    <name>TWME Repository</name>
    <url>https://repo.twme.dev/snapshots</url>
</repository>
```

```xml
<dependency>
    <groupId>dev.twme</groupId>
    <artifactId>TextDisplayShape</artifactId>
    <version>1.0.6</version>
</dependency>
```

## Usage

> **Note**: When providing a `Location` as the origin, please avoid including the pitch and yaw (set them to 0). Passing a location with rotation data (e.g., `player.getLocation()`) may cause unexpected behavior in shape orientation.

### Bukkit Mode (Direct Entity)

```java
// Triangle
Shape triangle = TextDisplayShapes.bukkit()
    .triangle(playerLocation, p1, p2, p3)
    .color(Color.fromARGB(150, 50, 100, 100))
    .doubleSided(true)
    .build();
triangle.spawn();

// Line
Shape line = TextDisplayShapes.bukkit()
    .line(playerLocation, p1, p2, 0.1f)
    .rollDegrees(90f)  // Rotate for visibility from different angles
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
Shape polyline = TextDisplayShapes.bukkit()
    .polyline(playerLocation, points, 0.1f)
    .closed(true)  // Connect last point to first
    .build();
polyline.spawn();

// Parallelogram
Shape parallelogram = TextDisplayShapes.bukkit()
    .parallelogram(playerLocation, p1, p2, p3)
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

// Create shape
Shape line = TextDisplayShapes.packet()
    .line(playerLocation, p1, p2, 0.1f)
    .color(Color.RED)
    .build();

// Add viewers (only these players will see the shape)
line.addViewer(player);
line.spawn();

// Remove viewer
line.removeViewer(player);

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
| `addViewer(Player)` | Add a viewer (packet mode only) |
| `removeViewer(Player)` | Remove a viewer (packet mode only) |
| `getViewers()` | Get all viewers |
| `getEntityUUIDs()` | Get UUIDs of all entities in this shape |
| `getEntities()` | Get all entity instances |

### Builder Options

| Method | Description |
|--------|-------------|
| `.color(Color)` | Set background color (ARGB) |
| `.doubleSided(boolean)` | Enable double-sided rendering |
| `.brightness(int block, int sky)` | Set brightness (0-15) |
| `.seeThrough(boolean)` | Make visible through blocks |
| `.roll(float)` | Line roll angle in radians |
| `.rollDegrees(float)` | Line roll angle in degrees |
| `.closed(boolean)` | Close polyline (connect last to first) |

## JavaDoc

You can find the JavaDoc [here](https://repo.twme.dev/javadoc/snapshots/dev/twme/TextDisplayShape/1.0.6).

## Dependencies

- Spigot/Paper 1.21+
- [PacketEvents](https://github.com/retrooper/packetevents) (for packet mode)
- [EntityLib](https://github.com/Tofaa2/EntityLib) (for packet mode)

## License

Apache License Version 2.0
