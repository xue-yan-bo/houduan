package com.jlm.homework.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A403通讯协议数据解析器
 * 专门用于解析A403协议的手写数据并转换为canvas绘制格式
 */
public class A403Parser {
    Logger logger = LoggerFactory.getLogger(A403Parser.class);
    // 硬件分辨率常量
    public static final class HardwareResolution {
        public static final int MAX_X = 21000;
        public static final int MAX_Y = 29700;
        public static final int MAX_PRESSURE = 8191;
    }

    // A4纸张尺寸（毫米）
    public static final class A4PaperSize {
        public static final int WIDTH_MM = 210;
        public static final int HEIGHT_MM = 297;
    }

    // A4纸张尺寸（像素，96 DPI）
    public static final class A4PaperPixels {
        public static final int WIDTH = 794;   // 210mm * 96 DPI / 25.4mm
        public static final int HEIGHT = 1123; // 297mm * 96 DPI / 25.4mm
    }

    // 坐标换算比例
    public static final class CoordinateScale {
        public static final double X = (double) A4PaperPixels.WIDTH / HardwareResolution.MAX_X;
        public static final double Y = (double) A4PaperPixels.HEIGHT / HardwareResolution.MAX_Y;
    }

    // 点位记录
    private static List<A403HandwritePoint> pointLog = new ArrayList<>();

    /**
     * 手写点数据结构
     */
    public static class A403HandwritePoint {
        public int x;
        public int y;
        public int pressure;
        public Long timestamp; // 可选时间戳字段，用于点位记录

        public A403HandwritePoint(int x, int y, int pressure) {
            this.x = x;
            this.y = y;
            this.pressure = pressure;
        }

        public A403HandwritePoint(int x, int y, int pressure, Long timestamp) {
            this.x = x;
            this.y = y;
            this.pressure = pressure;
            this.timestamp = timestamp;
        }
    }

    /**
     * 数据包结构
     */
    public static class A403Packet {
        public List<Integer> header;    // 包头 (0x55, 0x56, 0x0B, 0x01)
        public List<Integer> data;      // 数据内容
        public int checksum;            // 校验和
        public List<Integer> rawData;   // 原始数据
        public boolean isValid;         // 数据包是否有效
        public String errorMessage;     // 错误信息
    }

    /**
     * 解析后的数据结构
     */
    public static class A403ParsedData {
        public List<A403Packet> packets;
        public List<A403HandwritePoint> handwritePoints;
        public A403Statistics statistics;
    }

    /**
     * 统计数据
     */
    public static class A403Statistics {
        public int totalPackets;
        public int validPackets;
        public int invalidPackets;
        public int totalPoints;
        public double averagePressure;
        public CoordinateRange coordinateRange;
    }

    /**
     * 坐标范围
     */
    public static class CoordinateRange {
        public int minX;
        public int maxX;
        public int minY;
        public int maxY;
    }

    /**
     * 记录点位信息
     *
     * @param point 手写点
     */
    public static void logPoint(A403HandwritePoint point) {
        pointLog.add(new A403HandwritePoint(
                point.x,
                point.y,
                point.pressure,
                System.currentTimeMillis()
        ));
    }

    /**
     * 获取点位记录
     *
     * @return 点位记录数组的副本
     */
    public static List<A403HandwritePoint> getPointLog() {
        return new ArrayList<>(pointLog);
    }

    /**
     * 计算统计信息
     *
     * @param packets 数据包数组
     * @param points  手写点数组
     * @return 统计信息
     */
    public static A403Statistics calculateStatistics(List<A403Packet> packets,
                                                     List<A403HandwritePoint> points) {
        A403Statistics stats = new A403Statistics();

        // 计算有效包数量
        int validPackets = 0;
        for (A403Packet packet : packets) {
            if (packet.isValid) {
                validPackets++;
            }
        }

        // 计算总压力和平均压力
        int totalPressure = 0;
        for (A403HandwritePoint point : points) {
            totalPressure += point.pressure;
        }

        // 初始化坐标范围
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        // 计算坐标范围
        for (A403HandwritePoint point : points) {
            minX = Math.min(minX, point.x);
            maxX = Math.max(maxX, point.x);
            minY = Math.min(minY, point.y);
            maxY = Math.max(maxY, point.y);
        }

        // 设置统计信息
        stats.totalPackets = packets.size();
        stats.validPackets = validPackets;
        stats.invalidPackets = packets.size() - validPackets;
        stats.totalPoints = points.size();
        stats.averagePressure = points.isEmpty() ? 0 : (double) totalPressure / points.size();

        // 设置坐标范围
        stats.coordinateRange.minX = minX == Integer.MAX_VALUE ? 0 : minX;
        stats.coordinateRange.maxX = maxX == Integer.MIN_VALUE ? 0 : maxX;
        stats.coordinateRange.minY = minY == Integer.MAX_VALUE ? 0 : minY;
        stats.coordinateRange.maxY = maxY == Integer.MIN_VALUE ? 0 : maxY;

        return stats;
    }


    public static A403ParsedData parse(byte[] bytes) {
        // 将字节数组转换为整数列表（无符号处理）
        List<Integer> byteList = new ArrayList<>();
        for (byte b : bytes) {
            byteList.add(b & 0xFF); // 处理Java字节的有符号性
        }

        // 解析数据包
        List<A403Packet> packets = extractPackets(byteList);

        // 提取手写点
        List<A403HandwritePoint> handwritePoints = extractHandwritePoints(packets);

        // 计算统计信息
        A403Statistics statistics = calculateStatistics(packets, handwritePoints);

        // 构建返回对象
        A403ParsedData parsedData = new A403ParsedData();
        parsedData.packets = packets;
        parsedData.handwritePoints = handwritePoints;
        parsedData.statistics = statistics;

        return parsedData;
    }

    // 辅助方法：将字节数组转换为无符号整数列表
    private static List<Integer> convertBytesToUnsigned(byte[] bytes) {
        List<Integer> result = new ArrayList<>(bytes.length);
        for (byte b : bytes) {
            result.add(b & 0xFF);
        }
        return result;
    }


    public static List<A403Packet> extractPackets(List<Integer> bytes) {
        List<A403Packet> tempPackets = new ArrayList<>();
        int offset = 0;

        // 按正常顺序提取所有数据包
        while (offset < bytes.size()) {
            // 查找包头 (0x55, 0x56)
            int headerIndex = findHeader(bytes, offset);
            if (headerIndex == -1) break;

            // 确保有足够的数据来解析数据长度
            if (headerIndex + 3 >= bytes.size()) break;

            A403Packet packet = parsePacket(bytes, headerIndex);
            if (packet != null) {
                tempPackets.add(packet);
                offset = headerIndex + packet.rawData.size();
            } else {
                offset = headerIndex + 1;
            }
        }

        // 倒序排列数据包：最后一个数据包在前，第一个数据包在后
        Collections.reverse(tempPackets);
        return tempPackets;
    }

    /**
     * 查找包头位置 (0x55, 0x56)
     *
     * @param bytes       字节列表
     * @param startOffset 开始查找的偏移量
     * @return 包头位置索引，找不到返回-1
     */
    private static int findHeader(List<Integer> bytes, int startOffset) {
        for (int i = startOffset; i < bytes.size() - 1; i++) {
            if (bytes.get(i) == 0x55 && bytes.get(i + 1) == 0x56) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 解析单个数据包
     *
     * @param bytes      字节列表
     * @param startIndex 包头开始位置
     * @return 解析后的数据包，无效则返回null
     */
    private static A403Packet parsePacket(List<Integer> bytes, int startIndex) {
        // 确保有足够的数据（至少4字节包头+数据+校验和）
        if (startIndex + 4 > bytes.size()) {
            return null;
        }

        A403Packet packet = new A403Packet();
        packet.header = new ArrayList<>(bytes.subList(startIndex, startIndex + 4));

        // 获取数据长度（协议特定逻辑）
        int dataLength = getDataLength(bytes, startIndex);

        // 检查数据长度是否有效
        if (dataLength <= 0 || startIndex + 4 + dataLength + 1 > bytes.size()) {
            packet.isValid = false;
            packet.errorMessage = "Invalid data length";
            return packet;
        }

        // 提取数据部分
        int dataStart = startIndex + 4;
        int dataEnd = dataStart + dataLength;
        packet.data = new ArrayList<>(bytes.subList(dataStart, dataEnd));

        // 提取校验和
        packet.checksum = bytes.get(dataEnd) & 0xFF;

        // 提取原始数据（包头+数据+校验和）
        packet.rawData = new ArrayList<>(bytes.subList(startIndex, dataEnd + 1));

        // 验证数据包有效性
        packet.isValid = validatePacket(packet);
        if (!packet.isValid) {
            packet.errorMessage = "Checksum validation failed";
        }

        return packet;
    }

    /**
     * 根据协议规范获取数据长度（需根据实际协议实现）
     */
    private static int getDataLength(List<Integer> bytes, int startIndex) {
        // 示例：假设第三个字节是数据长度
        // 实际实现应根据A403协议规范
        return bytes.get(startIndex + 2) & 0xFF;
    }

    /**
     * 验证数据包校验和（需根据实际协议实现）
     */
    private static boolean validatePacket(A403Packet packet) {
        // 示例：简单的校验和验证
        // 实际实现应根据A403协议规范
        int sum = 0;
        for (int b : packet.data) {
            sum += b;
        }
        return (sum & 0xFF) == packet.checksum;
    }

    public static List<A403HandwritePoint> extractHandwritePoints(List<A403Packet> packets) {
        List<A403HandwritePoint> points = new ArrayList<>();

        for (A403Packet packet : packets) {
            if (packet.isValid && packet.data.size() >= 8) {
                // 检查数据类型：0x01通常表示手写数据
                int dataType = packet.data.get(0);
                if (dataType == 0x01) {
                    try {
                        // 大端序读取坐标和压力值（高位在前）
                        int x = (packet.data.get(1) << 8) | packet.data.get(2);
                        int y = (packet.data.get(3) << 8) | packet.data.get(4);

                        // 压力值：13位二进制数，最大值8191
                        int pressureRaw = (packet.data.get(5) << 8) | packet.data.get(6);
                        int pressure = pressureRaw & 0x1FFF; // 只取低13位 (0x1FFF = 8191)

                        // 验证坐标范围
                        if (x >= 0 && x <= HardwareResolution.MAX_X &&
                                y >= 0 && y <= HardwareResolution.MAX_Y &&
                                pressure >= 0 && pressure <= HardwareResolution.MAX_PRESSURE) {

                            A403HandwritePoint point = new A403HandwritePoint(x, y, pressure);
                            points.add(point);
                            System.out.printf("✅ 解析手写点: X=%d, Y=%d, 压力=%d\n", x, y, pressure);
                        } else {
                            System.out.printf("⚠️ 坐标超出范围: X=%d, Y=%d, 压力=%d\n", x, y, pressure);
                        }
                    } catch (Exception e) {
                        System.err.println("❌ 解析手写点失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else if (dataType == 0x02) {
                    // 按键数据包
                    StringBuilder sb = new StringBuilder("⌨️ 按键数据包: ");
                    for (int i = 1; i < packet.data.size(); i++) {
                        sb.append(String.format("0x%02X ", packet.data.get(i)));
                    }
                    System.out.println(sb.toString().trim());
                } else {
                    // 其他类型的数据包
                    System.out.printf("🔍 未知数据类型: 0x%02X\n", dataType);
                }
            }
        }

        return points;
    }

    /**
     * 将手写点数据转换为canvas绘制路径
     * @param points 手写点数组
     * @param canvasWidth canvas宽度
     * @param canvasHeight canvas高度
     * @returns 绘制路径数组
     */
    public static List<List<A403HandwritePoint>> convertToCanvasPaths(A403HandwritePoint[] points,Double canvasWidth,Double  canvasHeight) {
        if (points.length == 0) return new ArrayList<>();
        // 使用A4纸张的换算比例
        Double scaleX = CoordinateScale.X;
        Double scaleY = CoordinateScale.Y;
        // 转换为canvas坐标
        // 直接使用原始坐标，不进行Y轴翻转
        A403HandwritePoint[] canvasPoints = Arrays.stream(points).map(
                point->{
                    int x = point.x * scaleX.intValue();
                    int y = point.y * scaleY.intValue();
                    int pressure = point.pressure;
                    return new A403HandwritePoint(x, y, pressure);
                }
        ).toArray(A403HandwritePoint[]::new);
        List<List<A403HandwritePoint>> paths = new ArrayList<>();
        List<A403HandwritePoint> currentPath = new ArrayList<>();
        for (int i = 0; i < canvasPoints.length; i++) {
            A403HandwritePoint point = canvasPoints[i];
            // 判断是否应该开始新笔画
            Boolean shouldStartNewPath = false;
            // 1. 压力值为0表示一笔的结束
            if (point.pressure == 0) {
                shouldStartNewPath = true;
            }
            // 2. 如果当前路径为空，开始新路径
            else if (currentPath.size() == 0) {
                shouldStartNewPath = false; // 不开始新路径，直接添加点
            }
            // 3. 检查与前一个点的距离，如果距离过大可能是新笔画
            else if (currentPath.size() > 0) {
                A403HandwritePoint prevPoint = currentPath.get(currentPath.size() - 1);
                double distance = Math.sqrt(
                        Math.pow(point.x - prevPoint.x, 2) + Math.pow(point.y - prevPoint.y, 2)
                );

                // 如果距离超过阈值，可能是新笔画
                int distanceThreshold = 20; // 20像素的距离阈值
                if (distance > distanceThreshold) {
                    shouldStartNewPath = true;
                }

                // 4. 检查角度变化，如果角度变化过大可能是新笔画
                if (currentPath.size() >= 2 && !shouldStartNewPath) {
                    A403HandwritePoint prevPrevPoint = currentPath.get(currentPath.size() - 2);
                    double angle1 = Math.atan2(prevPoint.y - prevPrevPoint.y, prevPoint.x - prevPrevPoint.x);
                    double angle2 = Math.atan2(point.y - prevPoint.y, point.x - prevPoint.x);
                    double angleDiff = Math.abs(angle2 - angle1);

                    // 如果角度变化超过阈值，可能是新笔画
                    double angleThreshold = Math.PI / 2; // 90度
                    if (angleDiff > angleThreshold && angleDiff < Math.PI * 1.5) { // 排除接近180度的情况
                        shouldStartNewPath = true;
                    }
                }
            }
            //不是新路径，把点加入当前路径
            if(!shouldStartNewPath) {
                currentPath.add(point);
            }
            // 保存当前路径并开始新路径
            if (shouldStartNewPath&&currentPath.size() > 0) {
                paths.add(currentPath);
                currentPath = new ArrayList<>();
                currentPath.add(point);
            }

        }
        // 添加最后一个路径（如果有的话）
        if (currentPath.size() > 0) {
            paths.add(currentPath);
        }
        return paths;
    }

    /**
     * 将字节数组转换为可读字符串
     * @param bytes 字节数组
     * @returns 可读字符串
     */
    public static String bytesToString(byte[] bytes){
        try {
            // 尝试使用 UTF-8 编码
            String buffer = new String(bytes, "UTF-8");
            return buffer;
        } catch (UnsupportedEncodingException e) {
            try {
                // 如果 UTF-8 失败，尝试 ISO 编码
                String  buffer = new String(bytes, "ISO-8859-1");
                return buffer;
            } catch (UnsupportedEncodingException e1) {
                // 如果都失败，返回十六进制字符串
                String  buffer = new String(bytes, StandardCharsets.US_ASCII);
                return buffer;
            }
        }
    }

    /**
     * 检查字节数组是否包含中文字符
     * @param bytes 字节数组
     * @returns 是否包含中文字符
     */
    public static boolean  containsChineseCharacters(byte[] bytes) {
        // 中文字符的 UTF-8 编码范围
        for (int i = 0; i < bytes.length - 2; i++) {
            if (bytes[i] == 0xE4 && bytes[i + 1] == 0xB8 && bytes[i + 2] == 0xAD) {
                return true; // 中
            }
            if (bytes[i] == 0xE5 && bytes[i + 1] == 0x9B && bytes[i + 2] == 0xBD) {
                return true; // 国
            }
            if (bytes[i] == 0xE5 && bytes[i + 1] == 0xB1 && bytes[i + 2] == 0xB1) {
                return true; // 山
            }
            if (bytes[i] == 0xE8 && bytes[i + 1] == 0xA5 && bytes[i + 2] == 0xBF) {
                return true; // 西
            }
            if (bytes[i] == 0xE5 && bytes[i + 1] == 0xA4 && bytes[i + 2] == 0xAA) {
                return true; // 太
            }
            if (bytes[i] == 0xE5 && bytes[i + 1] == 0x8E && bytes[i + 2] == 0x86) {
                return true; // 原
            }
        }
        return false;
    }
}