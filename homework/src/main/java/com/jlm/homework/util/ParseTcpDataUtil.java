package com.jlm.homework.util;

import com.jlm.homework.socket.*;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 基于A4通讯协议1.1.pdf解析手写数据TCP数据包
 */
public class ParseTcpDataUtil {
    /**
     * 整体解析，主要是获取类型
     * @param data 待解析的TCP包数据
     * @return
     */
    public static ParseResult parseTypeTcpPacket(byte[] data){
        // 解析Header（包头），协议定义为[0x55, 0x56]
        byte header0 = data[0];
        byte header1 = data[1];
        if (header0 != 0x55 || header1 != 0x56) {
            throw new IllegalArgumentException("包头不匹配，协议要求包头为0x55、0x56");
        }

        // 解析Length（数据长度）：Type长度(1字节) + Packet长度
        int length = data[2] & 0xFF; // 转为无符号整数

        // 解析Type（数据类型）：0x01为手写数据，0x02为按键数据，0x03为序列号
        byte type = data[3];
        if (type != 0x01 && type != 0x02 && type != 0x03) {
            throw new IllegalArgumentException("数据类型异常，当前为0x" + Integer.toHexString(type & 0xFF) + ", 需为0x01、0x02或0x03");
        }

        // 解析Checksum（校验和）：前面所有字节的累加和
        byte checksum = data[data.length - 1];
        int calculatedChecksum = calculateChecksum(data, 0, data.length - 1);
        boolean checksumValid = (calculatedChecksum & 0xFF) == (checksum & 0xFF);

        // 封装解析结果
        ParseResult result = new ParseResult();
        result.setHeader(new byte[]{header0, header1});
        result.setLength(length);
        result.setType(type);
        result.setChecksum(checksum);
        result.setChecksumValid(checksumValid);
        return result;
    }

    /**
     * 解析手写数据TCP数据包
     * @param data 待解析的TCP包数据
     * @return 解析结果对象列表，可能包含多条手写数据
     * @throws IllegalArgumentException 数据异常时抛出
     */
    public static List<HandwritingParseResult> parseHandwritingTcpPackets(byte[] data) {
        // 基础合法性校验
        if (data == null || data.length < 5) { // 最小长度：Header(2)+Length(1)+Type(1)+Checksum(1)
            throw new IllegalArgumentException("数据长度异常，至少需为5字节");
        }

        // 解析Header
        byte header0 = data[0];
        byte header1 = data[1];
        if (header0 != 0x55 || header1 != 0x56) {
            throw new IllegalArgumentException("包头不匹配");
        }

        // 解析Length和Type
        int length = data[2] & 0xFF;
        byte type = data[3];
        if (type != 0x01) {
            throw new IllegalArgumentException("数据类型异常，需为0x01");
        }

        // 验证总长度
        if (data.length != length + 4) { // Header(2) + Length(1) + Type(1) + Packet(length-1) + Checksum(1) = length + 2
            throw new IllegalArgumentException("数据总长度与Length字段不匹配");
        }

        // 解析Checksum
        byte checksum = data[data.length - 1];
        int calculatedChecksum = calculateChecksum(data, 0, data.length - 1);
        boolean checksumValid = (calculatedChecksum & 0xFF) == (checksum & 0xFF);

        // 提取Packet数据
        byte[] packet = Arrays.copyOfRange(data, 4, data.length - 1);

        // 解析多条手写数据（每条10字节：X(2)+Y(2)+压力值(2)+时间戳(4)）
        List<HandwritingParseResult> results = new ArrayList<>();
        int singleDataLength = 10;
        for (int i = 0; i + singleDataLength <= packet.length; i += singleDataLength) {
            byte[] singlePacket = Arrays.copyOfRange(packet, i, i + singleDataLength);

            // 解析X坐标
            int x = ((singlePacket[1] & 0xFF) << 8) | (singlePacket[0] & 0xFF);
            // 解析Y坐标
            int y = ((singlePacket[3] & 0xFF) << 8) | (singlePacket[2] & 0xFF);
            // 解析压力值
            int pressure = ((singlePacket[5] & 0xFF) << 8) | (singlePacket[4] & 0xFF);
            // 解析时间戳
            long timestamp = ((long) (singlePacket[9] & 0xFF) << 24)
                    | ((long) (singlePacket[8] & 0xFF) << 16)
                    | ((long) (singlePacket[7] & 0xFF) << 8)
                    | (singlePacket[6] & 0xFF);

            // 封装结果
            HandwritingParseResult result = new HandwritingParseResult();
            result.setHeader(new byte[]{header0, header1});
            result.setLength(length);
            result.setType(type);
            result.setX(x);
            result.setY(y);
            result.setPressure(pressure);
            result.setTimestamp(timestamp);
            result.setChecksum(checksum);
            result.setChecksumValid(checksumValid);

            results.add(result);
        }

        return results;
    }

    /**
     * 解析按键数据TCP数据包
     * @param data 待解析的TCP包数据
     * @return 解析结果对象列表，可能包含多条按键数据
     * @throws IllegalArgumentException 数据异常时抛出
     */
    public static ButtonParseResult parseButtonTcpPackets(byte[] data) {
        // 基础合法性校验
        if (data == null || data.length < 5) {
            throw new IllegalArgumentException("数据长度异常，至少需为5字节");
        }

        // 解析Header
        byte header0 = data[0];
        byte header1 = data[1];
        if (header0 != 0x55 || header1 != 0x56) {
            throw new IllegalArgumentException("包头不匹配");
        }

        // 解析Length和Type
        int length = data[2] & 0xFF;
        byte type = data[3];
        if (type != 0x02) {
            throw new IllegalArgumentException("数据类型异常，需为0x02");
        }

        // 验证总长度
        if (data.length != length + 4) {
            throw new IllegalArgumentException("数据总长度与Length字段不匹配");
        }

        // 解析Checksum
        byte checksum = data[data.length - 1];
        int calculatedChecksum = calculateChecksum(data, 0, data.length - 1);
        boolean checksumValid = (calculatedChecksum & 0xFF) == (checksum & 0xFF);

        // 提取Packet数据
        byte[] packet = Arrays.copyOfRange(data, 4, data.length - 1);

        // 解析多条按键数据（每条6字节：按键码(2)+时间戳(4)）
        int singleDataLength = 6;
        // 解析按键码（16位，每一位代表一个按键）
        int buttonCode = ((packet[1] & 0xFF) << 8) | (packet[0] & 0xFF);

        // 解析时间戳
        /*long timestamp = ((long) (packet[5] & 0xFF) << 24)
                | ((long) (packet[4] & 0xFF) << 16)
                | ((long) (packet[3] & 0xFF) << 8)
                | (packet[2] & 0xFF);*/

        // 封装结果
        ButtonParseResult result = new ButtonParseResult();
        result.setHeader(new byte[]{header0, header1});
        result.setLength(length);
        result.setType(type);
        result.setButton(buttonCode);
        //result.setTimestamp(timestamp);
        result.setChecksum(checksum);
        result.setChecksumValid(checksumValid);


        result.setButtonState(true);



        return result;
    }

    /**
     * 解析序列号TCP数据包
     * @param data 待解析的TCP包数据
     * @return 解析结果对象
     * @throws IllegalArgumentException 数据异常时抛出
     */
    public static MacParseResult parseSerialNumberTcpPacket(byte[] data) {
        // 基础合法性校验
        if (data == null || data.length < 5) {
            throw new IllegalArgumentException("数据长度异常，至少需为5字节");
        }

        // 解析Header
        byte header0 = data[0];
        byte header1 = data[1];
        if (header0 != 0x55 || header1 != 0x56) {
            throw new IllegalArgumentException("包头不匹配");
        }

        // 解析Length和Type
        int length = data[2] & 0xFF;
        byte type = data[3];
        if (type != 0x03) {
            throw new IllegalArgumentException("数据类型异常，需为0x03");
        }

        // 验证总长度
        if (data.length != length + 4) {
            throw new IllegalArgumentException("数据总长度与Length字段不匹配");
        }

        // 解析Checksum
        byte checksum = data[data.length - 1];
        int calculatedChecksum = calculateChecksum(data, 0, data.length - 1);
        boolean checksumValid = (calculatedChecksum & 0xFF) == (checksum & 0xFF);

        // 提取Packet数据（序列号）
        byte[] packet = Arrays.copyOfRange(data, 4, data.length - 1);
        Integer serialNumber = byteArrayToInt(packet);

        // 封装结果
        MacParseResult result = new MacParseResult();
        result.setHeader(new byte[]{header0, header1});
        result.setLength(length);
        result.setType(type);
        result.setMac(serialNumber); // 保持使用现有字段存储序列号
        result.setChecksum(checksum);
        result.setChecksumValid(checksumValid);

        return result;
    }

    /**
     * 解析序列号TCP数据包
     * @param data 待解析的TCP包数据
     * @return 解析结果对象
     * @throws IllegalArgumentException 数据异常时抛出
     */
    public static SubjectParseResult parseSubjectTcpPacket(byte[] data) throws UnsupportedEncodingException {
        // 基础合法性校验
        if (data == null || data.length < 5) {
            throw new IllegalArgumentException("数据长度异常，至少需为5字节");
        }

        // 解析Header
        byte header0 = data[0];
        byte header1 = data[1];
        if (header0 != 0x55 || header1 != 0x56) {
            throw new IllegalArgumentException("包头不匹配");
        }

        // 解析Length和Type
        int length = data[2] & 0xFF;
        byte type = data[3];
        if (type != 0x03) {
            throw new IllegalArgumentException("数据类型异常，需为0x03");
        }

        // 验证总长度
        if (data.length != length + 4) {
            throw new IllegalArgumentException("数据总长度与Length字段不匹配");
        }

        // 解析Checksum
        byte checksum = data[data.length - 1];
        int calculatedChecksum = calculateChecksum(data, 0, data.length - 1);
        boolean checksumValid = (calculatedChecksum & 0xFF) == (checksum & 0xFF);

        // 提取Packet数据（序列号）
        byte[] packet = Arrays.copyOfRange(data, 4, data.length - 1);
        // 使用Unicode解码（UTF-16LE）
        String subject = new String(packet, "UTF-16LE");

        // 封装结果
        SubjectParseResult result = new SubjectParseResult();
        result.setHeader(new byte[]{header0, header1});
        result.setLength(length);
        result.setType(type);
        result.setSubject(subject); // 保持使用现有字段存储序列号
        result.setChecksum(checksum);
        result.setChecksumValid(checksumValid);

        return result;
    }
    /**
     * 计算指定字节数组范围内的累加和（协议校验和计算逻辑）
     * @param data 待计算数组
     * @param start 起始索引（包含）
     * @param end 结束索引（包含）
     * @return 累加和
     */
    private static int calculateChecksum(byte[] data, int start, int end) {
        int sum = 0;
        for (int i = start; i <= end; i++) {
            sum += (data[i] & 0xFF); // 转为无符号整数累加
        }
        return sum;
    }

    // 测试方法
    public static void main(String[] args) {
        // 测试手写数据解析
        byte[] handwritingData = {0x55, 0x56, 0x0B, 0x01, 0x27, 0x24, 0x58, 0x19, (byte)0xE8, 0x1C, 0x62, 0x00, 0x00, 0x00, (byte)0xD9};
        try {
            List<HandwritingParseResult> handwritingResults = parseHandwritingTcpPackets(handwritingData);
            System.out.println("手写数据解析结果:");
            for (HandwritingParseResult result : handwritingResults) {
                System.out.println(result);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("手写数据解析失败：" + e.getMessage());
        }

        // 测试按键数据解析
        byte[] buttonData = {0x55, 0x56, 0x07, 0x02, 0x01, 0x00, 0x00, 0x00, 0x00, 0x01, 0x08};
        try {
            ButtonParseResult buttonResult = parseButtonTcpPackets(buttonData);
            System.out.println("按键数据解析结果:");
            System.out.println(buttonResult.toString());
            System.out.println("按键状态:");
            System.out.println("按键 " + buttonResult.getButton() + ": " + (buttonResult.isButtonState()? "按下" : "弹起"));

        } catch (IllegalArgumentException e) {
            System.err.println("按键数据解析失败：" + e.getMessage());
        }


    }
    public static int byteArrayToInt(byte[] byteArray) {
        int number = 0;
        for (int i = 0; i < byteArray.length; i++) {
            number |= (byteArray[i] & 0xff) << (i * 8);
        }
        return number;
    }
}