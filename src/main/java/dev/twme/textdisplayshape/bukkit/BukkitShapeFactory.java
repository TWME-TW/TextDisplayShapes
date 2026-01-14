package dev.twme.textdisplayshape.bukkit;

import org.bukkit.Location;
import org.joml.Vector3f;

/**
 * Bukkit 形狀的工廠類別。
 * 使用直接操作實體的方式來顯示形狀。
 */
public class BukkitShapeFactory {

    /**
     * 創建三角形建造者。
     *
     * @param origin 生成位置（通常是玩家位置）
     * @param p1     第一個頂點（世界座標）
     * @param p2     第二個頂點（世界座標）
     * @param p3     第三個頂點（世界座標）
     * @return 三角形建造者
     */
    public BukkitTriangle.Builder triangle(Location origin, Vector3f p1, Vector3f p2, Vector3f p3) {
        return new BukkitTriangle.Builder(origin, p1, p2, p3);
    }

    /**
     * 創建線條建造者。
     *
     * @param origin    生成位置（通常是玩家位置）
     * @param p1        線條起點（世界座標）
     * @param p2        線條終點（世界座標）
     * @param thickness 線條粗細
     * @return 線條建造者
     */
    public BukkitLine.Builder line(Location origin, Vector3f p1, Vector3f p2, float thickness) {
        return new BukkitLine.Builder(origin, p1, p2, thickness);
    }

    /**
     * 創建平行四邊形建造者。
     *
     * @param origin 生成位置（通常是玩家位置）
     * @param p1     起點角（世界座標）
     * @param p2     第二個點，定義寬度方向（世界座標）
     * @param p3     第三個點，定義高度方向（世界座標）
     * @return 平行四邊形建造者
     */
    public BukkitParallelogram.Builder parallelogram(Location origin, Vector3f p1, Vector3f p2, Vector3f p3) {
        return new BukkitParallelogram.Builder(origin, p1, p2, p3);
    }
}
