package dev.twme.textdisplayshape.shape;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 所有形狀的基礎介面。
 * 定義了形狀的生命週期管理和觀察者管理方法。
 */
public interface Shape {

    /**
     * 生成形狀到世界中。
     */
    void spawn();

    /**
     * 從世界中移除形狀。
     */
    void remove();

    /**
     * 檢查形狀是否已生成。
     *
     * @return 如果已生成則返回 true
     */
    boolean isSpawned();

    /**
     * 添加可以看到此形狀的玩家（僅適用於封包模式）。
     *
     * @param player 玩家
     */
    void addViewer(Player player);

    /**
     * 移除可以看到此形狀的玩家（僅適用於封包模式）。
     *
     * @param player 玩家
     */
    void removeViewer(Player player);

    /**
     * 獲取所有可以看到此形狀的玩家。
     *
     * @return 玩家集合
     */
    Set<Player> getViewers();

    /**
     * 獲取此形狀所有實體的 UUID。
     * 一個形狀可能由多個 TextDisplay 實體組成（例如三角形由 3 個實體組成）。
     *
     * @return 實體 UUID 列表，如果尚未生成則返回空列表
     */
    List<UUID> getEntityUUIDs();
}
