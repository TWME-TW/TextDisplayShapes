package dev.twme.textdisplayshape.util;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 處理 TextDisplay 形狀渲染的核心計算工具類。
 * 支援三角形、線條和平行四邊形。
 */
public class TextDisplayUtil {

    /**
     * 創建一個自定義的錯切變換矩陣並與原矩陣相乘。
     */
    private static Matrix4f shear(Matrix4fc mat, float yx, float xy) {
        Matrix4f shearMatrix = new Matrix4f();
        shearMatrix.m10(yx);
        shearMatrix.m01(xy);
        return new Matrix4f(mat).mul(shearMatrix);
    }

    /**
     * 獲取單位正方形的變換矩陣。
     */
    public static Matrix4f getTextDisplayUnitSquare() {
        return new Matrix4f().translate(0.4F, 0.0F, 0.0F).scale(8.0F, 4.0F, 1.0F);
    }

    /**
     * 獲取左對齊的單位三角形變換矩陣列表。
     */
    public static List<Matrix4f> getTextDisplayUnitTriangle() {
        return Stream.of(
                new Matrix4f().scale(0.5f).mul(getTextDisplayUnitSquare()),
                shear(new Matrix4f().scale(0.5f).translate(1f, 0f, 0f), -1f, 0f).mul(getTextDisplayUnitSquare()),
                shear(new Matrix4f().scale(0.5f).translate(0f, 1f, 0f), 0f, -1f).mul(getTextDisplayUnitSquare()))
                .collect(Collectors.toList());
    }

    /**
     * 計算三角形的變換矩陣。
     *
     * @param point1 第一個頂點
     * @param point2 第二個頂點
     * @param point3 第三個頂點
     * @return 包含變換矩陣和相關資訊的結果物件
     */
    public static TextDisplayTriangleResult textDisplayTriangle(
            Vector3f point1,
            Vector3f point2,
            Vector3f point3) {
        Vector3f p2 = new Vector3f(point2).sub(point1);
        Vector3f p3 = new Vector3f(point3).sub(point1);

        if (new Vector3f(p2).cross(p3).lengthSquared() < 1.0E-4F) {
            p3.add(0.0001f, 0.0001f, 0.0001f);
        }

        Vector3f zAxis = new Vector3f(p2).cross(p3).normalize();
        Vector3f xAxis = new Vector3f(p2).normalize();
        Vector3f yAxis = new Vector3f(zAxis).cross(xAxis).normalize();

        float width = p2.length();
        float height = p3.dot(yAxis);
        float p3Width = p3.dot(xAxis);

        Quaternionf rotation = new Quaternionf().lookAlong(new Vector3f(zAxis).mul(-1f), yAxis).conjugate();

        float shear = (width > 0.001f) ? p3Width / width : 0.0f;

        Matrix4f transform = new Matrix4f()
                .translate(point1)
                .rotate(rotation)
                .scale(width, height, 1f);
        transform = shear(transform, shear, 0.0F);

        Matrix4f finalTransform = transform;
        List<Matrix4f> transforms = getTextDisplayUnitTriangle().stream()
                .map(unit -> new Matrix4f(finalTransform).mul(unit))
                .collect(Collectors.toList());

        return new TextDisplayTriangleResult(
                transforms, xAxis, yAxis, zAxis, height, width, rotation, shear);
    }

    /**
     * 計算線條的變換矩陣。
     * 線條是一個非常細的矩形，根據厚度自動置中。
     *
     * @param point1    線條起點
     * @param point2    線條終點
     * @param thickness 線條粗細
     * @return 變換矩陣
     */
    public static Matrix4f textDisplayLine(Vector3f point1, Vector3f point2, float thickness) {
        Vector3f direction = new Vector3f(point2).sub(point1);
        float length = direction.length();

        if (length < 0.001f) {
            return new Matrix4f();
        }

        // 找到一個垂直於線條方向的軸作為"上"方向
        Vector3f up = new Vector3f(0, 1, 0);
        if (Math.abs(direction.dot(up) / length) > 0.99f) {
            up = new Vector3f(1, 0, 0);
        }

        Vector3f zAxis = new Vector3f(direction).cross(up).normalize();
        Vector3f xAxis = new Vector3f(direction).normalize();
        Vector3f yAxis = new Vector3f(zAxis).cross(xAxis).normalize();

        Quaternionf rotation = new Quaternionf().lookAlong(new Vector3f(zAxis).mul(-1f), yAxis).conjugate();

        // 線條的變換：平移到起點，旋轉到正確方向，置中厚度，縮放到正確長度和粗細
        // translate(0, -0.5, 0) 將線條在 Y 方向置中（厚度方向）
        return new Matrix4f()
                .translate(point1)
                .rotate(rotation)
                .scale(length, thickness, 1f)
                .translate(0f, -0.5f, 0f) // 置中：向下偏移半個單位（因為單位正方形從 0 開始）
                .mul(getTextDisplayUnitSquare());
    }

    /**
     * 計算平行四邊形的變換矩陣。
     * 平行四邊形由三個點定義：p1 是起點角，p2 和 p3 定義兩條邊。
     * 第四個點自動計算為 p1 + (p2-p1) + (p3-p1)
     *
     * @param point1 起點（一個角）
     * @param point2 第二個點（定義第一條邊，寬度方向）
     * @param point3 第三個點（定義第二條邊，高度方向）
     * @return 變換矩陣
     */
    public static Matrix4f textDisplayParallelogram(Vector3f point1, Vector3f point2, Vector3f point3) {
        Vector3f p2 = new Vector3f(point2).sub(point1); // 寬度向量
        Vector3f p3 = new Vector3f(point3).sub(point1); // 高度向量

        // 處理共線情況
        if (new Vector3f(p2).cross(p3).lengthSquared() < 1.0E-4F) {
            p3.add(0.0001f, 0.0001f, 0.0001f);
        }

        Vector3f zAxis = new Vector3f(p2).cross(p3).normalize();
        Vector3f xAxis = new Vector3f(p2).normalize();
        Vector3f yAxis = new Vector3f(zAxis).cross(xAxis).normalize();

        float width = p2.length();
        float height = p3.dot(yAxis);
        float p3Width = p3.dot(xAxis);

        Quaternionf rotation = new Quaternionf().lookAlong(new Vector3f(zAxis).mul(-1f), yAxis).conjugate();

        // 計算錯切量（讓矩形變成平行四邊形）
        float shear = (width > 0.001f) ? p3Width / width : 0.0f;

        // 平行四邊形使用完整的單位正方形（不像三角形需要拼接）
        Matrix4f transform = new Matrix4f()
                .translate(point1)
                .rotate(rotation)
                .scale(width, height, 1f);
        transform = shear(transform, shear, 0.0F);

        return transform.mul(getTextDisplayUnitSquare());
    }
}
