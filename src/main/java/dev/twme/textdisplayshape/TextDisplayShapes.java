package dev.twme.textdisplayshape;

import dev.twme.textdisplayshape.bukkit.BukkitShapeFactory;
import dev.twme.textdisplayshape.packet.PacketShapeFactory;

/**
 * TextDisplayShapes 函式庫的主要入口點。
 * 提供兩種方式來顯示幾何形狀：
 * <ul>
 * <li>{@link #bukkit()} - 使用 Bukkit API 直接操作實體</li>
 * <li>{@link #packet()} - 使用 EntityLib 封包方式顯示</li>
 * </ul>
 *
 * <h2>使用範例 (Bukkit)</h2>
 * 
 * <pre>{@code
 * // 創建三角形
 * Shape triangle = TextDisplayShapes.bukkit()
 *         .triangle(playerLocation, p1, p2, p3)
 *         .color(Color.fromARGB(150, 50, 100, 100))
 *         .doubleSided(true)
 *         .build();
 * triangle.spawn();
 *
 * // 移除三角形
 * triangle.remove();
 * }</pre>
 *
 * <h2>使用範例 (Packet/EntityLib)</h2>
 * 
 * <pre>{@code
 * // 需要先初始化 EntityLib
 * // EntityLib.init(platform, settings);
 *
 * // 創建線條
 * Shape line = TextDisplayShapes.packet()
 *         .line(playerLocation, p1, p2, 0.1f)
 *         .color(Color.RED)
 *         .build();
 *
 * // 添加觀察者
 * line.addViewer(player);
 * line.spawn();
 *
 * // 移除線條
 * line.remove();
 * }</pre>
 */
public final class TextDisplayShapes {

    private static final BukkitShapeFactory BUKKIT_FACTORY = new BukkitShapeFactory();
    private static final PacketShapeFactory PACKET_FACTORY = new PacketShapeFactory();

    private TextDisplayShapes() {
        // 工具類別，禁止實例化
    }

    /**
     * 獲取 Bukkit 形狀工廠。
     * 使用此工廠創建的形狀會直接在伺服器端創建 TextDisplay 實體，
     * 所有玩家都可以看到。
     *
     * @return Bukkit 形狀工廠
     */
    public static BukkitShapeFactory bukkit() {
        return BUKKIT_FACTORY;
    }

    /**
     * 獲取封包形狀工廠。
     * 使用此工廠創建的形狀只會透過封包發送給指定的觀察者，
     * 伺服器端不會創建實際實體。
     *
     * <p>
     * <strong>注意：</strong>使用前需要先初始化 EntityLib：
     * </p>
     * 
     * <pre>{@code
     * SpigotEntityLibPlatform platform = new SpigotEntityLibPlatform(plugin);
     * APIConfig settings = new APIConfig(PacketEvents.getAPI())
     *         .tickTickables()
     *         .trackPlatformEntities();
     * EntityLib.init(platform, settings);
     * }</pre>
     *
     * @return 封包形狀工廠
     */
    public static PacketShapeFactory packet() {
        return PACKET_FACTORY;
    }
}
