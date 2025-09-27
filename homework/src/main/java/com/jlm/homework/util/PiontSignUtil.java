package com.jlm.homework.util;

import com.jlm.homework.entity.AuditCoordinate;
import com.jlm.homework.entity.AuditLogoCoordinate;

import java.util.List;

public class PiontSignUtil {

    public static double distance(AuditLogoCoordinate p1, AuditLogoCoordinate p2) {
        return Math.sqrt(Math.pow(p2.getX() - p1.getX(), 2) + Math.pow(p2.getY() - p1.getY(), 2));
    }
    
    public static double angle(AuditLogoCoordinate p1, AuditLogoCoordinate p2, AuditLogoCoordinate p3) {
        double dx1 = p2.getX() - p1.getX();
        double dy1 = p2.getY() - p1.getY();
        double dx2 = p3.getX() - p2.getX();
        double dy2 = p3.getY() - p2.getY();
        double dotProduct = dx1 * dx2 + dy1 * dy2;
        double magnitude1 = Math.sqrt(dx1 * dx1 + dy1 * dy1);
        double magnitude2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);
        double cosTheta = dotProduct / (magnitude1 * magnitude2);
        return Math.toDegrees(Math.acos(cosTheta));
    }
    
    public static boolean isCheckMark(List<AuditLogoCoordinate> points) {
        if (points.size() != 4) return false; // “√”需要4个点（不包括起点重复）
        double[] expectedDistances = {5, 5, Math.sqrt(34), Math.sqrt(34)}; // 预期距离需要根据具体点来设定，这里仅为示例值，实际应根据具体点坐标计算得出。
        double[] expectedAngles = {90, 90, 60}; // 预期角度，同样需要根据具体点来设定。这里仅为示意。
        for (int i = 0; i < points.size(); i++) {
            AuditLogoCoordinate p1 = points.get(i);
            AuditLogoCoordinate p2 = points.get((i + 1) % points.size()); // 闭环处理，最后一个点和第一个点比较。
            if (Math.abs(distance(p1, p2) - expectedDistances[i]) > 0.1 || Math.abs(angle(points.get((i + 2) % points.size()), p1, p2) - expectedAngles[i / 2]) > 1) { // 容差设置为0.1和1度。
                return false;
            }
        }
        return true; // 所有检查都通过则返回true。
    }
    
    // 新增方法：判断是否是×符号
    public static boolean isCrossMark(List<AuditLogoCoordinate> points) {
        // ×符号通常由两条交叉的线段组成，需要4个点
        if (points == null || (points.size() != 4 )) {
            return false;
        }
        
        // 处理5个点的情况（起点重复）
        List<AuditLogoCoordinate> actualPoints = points;

        
        // ×符号应该有两条交叉的线段，我们假设点的顺序是：左上->右下->右上->左下
        // 检查第一条对角线（左上到右下）
        AuditLogoCoordinate p1 = actualPoints.get(0);
        AuditLogoCoordinate p2 = actualPoints.get(1);
        double diagonal1Length = distance(p1, p2);
        
        // 检查第二条对角线（右上到左下）
        AuditLogoCoordinate p3 = actualPoints.get(2);
        AuditLogoCoordinate p4 = actualPoints.get(3);
        double diagonal2Length = distance(p3, p4);
        
        // 两条对角线长度应大致相等
        if (Math.abs(diagonal1Length - diagonal2Length) > diagonal1Length * 0.2) { // 20%容差
            return false;
        }
        
        // 检查两条对角线是否相交且大致垂直
        // 简化的方法是检查中心点是否接近
        AuditLogoCoordinate center1 = new AuditLogoCoordinate();
        center1.setX((p1.getX() + p2.getX()) / 2);
        center1.setY((p1.getY() + p2.getY()) / 2);
        AuditLogoCoordinate center2 = new AuditLogoCoordinate();
        center2.setX((p3.getX() + p4.getX()) / 2);
        center2.setY((p3.getY() + p4.getY()) / 2);
        
        if (distance(center1, center2) > diagonal1Length * 0.3) { // 中心点距离不应太大
            return false;
        }
        
        // 检查对角线之间的角度是否接近90度
        double angleBetweenDiagonals = calculateAngleBetweenLines(p1, p2, p3, p4);
        if (Math.abs(angleBetweenDiagonals - 90) > 20) { // 20度容差
            return false;
        }
        
        return true;
    }
    
    // 辅助方法：计算两条线段之间的角度
    private static double calculateAngleBetweenLines(AuditLogoCoordinate p1, AuditLogoCoordinate p2, AuditLogoCoordinate p3, AuditLogoCoordinate p4) {
        // 计算第一条线段的方向向量
        double dx1 = p2.getX() - p1.getX();
        double dy1 = p2.getY() - p1.getY();
        
        // 计算第二条线段的方向向量
        double dx2 = p4.getX() - p3.getX();
        double dy2 = p4.getY() - p3.getY();
        
        // 计算向量的点积
        double dotProduct = dx1 * dx2 + dy1 * dy2;
        
        // 计算向量的模长
        double magnitude1 = Math.sqrt(dx1 * dx1 + dy1 * dy1);
        double magnitude2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);
        
        // 计算夹角的余弦值
        double cosTheta = dotProduct / (magnitude1 * magnitude2);
        
        // 防止计算误差导致的NaN
        cosTheta = Math.max(-1.0, Math.min(1.0, cosTheta));
        
        // 转换为角度
        return Math.toDegrees(Math.acos(cosTheta));
    }
}
