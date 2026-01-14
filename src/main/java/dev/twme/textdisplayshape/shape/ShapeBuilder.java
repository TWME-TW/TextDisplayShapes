package dev.twme.textdisplayshape.shape;

import org.bukkit.Color;

/**
 * 形狀建造者的基礎介面。
 * 提供流暢的 API 來配置形狀的外觀屬性。
 *
 * @param <T> 建造的形狀類型
 */
public interface ShapeBuilder<T extends Shape> {

    /**
     * 設定形狀的背景顏色。
     *
     * @param color 顏色（包含 ARGB）
     * @return 此建造者
     */
    ShapeBuilder<T> color(Color color);

    /**
     * 設定是否為雙面顯示。
     *
     * @param doubleSided true 表示雙面
     * @return 此建造者
     */
    ShapeBuilder<T> doubleSided(boolean doubleSided);

    /**
     * 設定形狀的亮度。
     *
     * @param block 方塊光照等級 (0-15)
     * @param sky   天空光照等級 (0-15)
     * @return 此建造者
     */
    ShapeBuilder<T> brightness(int block, int sky);

    /**
     * 設定是否透視（可以透過方塊看到）。
     *
     * @param seeThrough true 表示可透視
     * @return 此建造者
     */
    ShapeBuilder<T> seeThrough(boolean seeThrough);

    /**
     * 建造形狀實例。
     *
     * @return 建造的形狀
     */
    T build();
}
