package com.jlm.homework.util;

import com.jlm.homework.socket.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
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

        // 解析Checksum（校验和）：前面所有字节的累加和，不包括校验和字节本身
        byte checksum = data[data.length - 1];
        int calculatedChecksum = calculateChecksum(data, 0, data.length - 2);
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
        if (data.length != length + 4) { // Header(2) + Length(1) + Type(1) + Packet(length-1) + Checksum(1) = length + 4
            throw new IllegalArgumentException("数据总长度与Length字段不匹配");
        }

        // 解析Checksum
        byte checksum = data[data.length - 1];
        int calculatedChecksum = calculateChecksum(data, 0, data.length - 2);
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
        int calculatedChecksum = calculateChecksum(data, 0, data.length - 2);
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
        int calculatedChecksum = calculateChecksum(data, 0, data.length - 2);
        boolean checksumValid = (calculatedChecksum & 0xFF) == (checksum & 0xFF);

        // 提取Packet数据（序列号）
        byte[] packet = Arrays.copyOfRange(data, 4, data.length - 1);
        // 将原始字节转换为十六进制字符串，确保所有字节都被正确保存
        String serialNumber = bytesToHexString(packet);
        // 使用Base64 + Hex编码序列号
        //String byteStr = encodeSerialNumber(packet);
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
     * 解析LCD显示字符串TCP数据包
     * @param data 待解析的TCP包数据
     * @return 解析结果对象
     * @throws IllegalArgumentException 数据异常时抛出
     * @throws UnsupportedEncodingException 编码异常
     */
    public static LCDDisplayParseResult parseLcdDisplayTcpPacket(byte[] data) throws UnsupportedEncodingException {
        // 基础合法性校验
        if (data == null || data.length < 5) {
            throw new IllegalArgumentException("数据长度异常，至少需为5字节");
        }

        // 解析Header
        byte header0 = data[0];
        byte header1 = data[1];
        if (header0 != 0x55 || header1 != 0x56) {
            throw new IllegalArgumentException("包头不匹配，协议要求包头为0x55、0x56");
        }

        // 解析Length和Type
        int length = data[2] & 0xFF;
        byte type = data[3];
        if (type != 0x04) {
            throw new IllegalArgumentException("数据类型异常，需为0x04（LCD显示字符串）");
        }

        // 验证总长度
        if (data.length != length + 4) {
            throw new IllegalArgumentException("数据总长度与Length字段不匹配");
        }

        // 解析Checksum
        byte checksum = data[data.length - 1];
        int calculatedChecksum = calculateChecksum(data, 0, data.length - 2);
        boolean checksumValid = (calculatedChecksum & 0xFF) == (checksum & 0xFF);

        // 提取Packet数据（显示字符串）
        byte[] packet = Arrays.copyOfRange(data, 4, data.length - 1);
        // 使用UTF-16LE解码（unicode编码）
        String displayString = new String(packet, "UTF-16LE");

        // 封装结果
        LCDDisplayParseResult result = new LCDDisplayParseResult();
        result.setHeader(new byte[]{header0, header1});
        result.setLength(length);
        result.setType(type);
        result.setDisplayString(displayString);
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
        /*byte[] handwritingData = {0x55, 0x56, 0x0B, 0x01, 0x27, 0x24, 0x58, 0x19, (byte)0xE8, 0x1C, 0x62, 0x00, 0x00, 0x00, (byte)0xD9};
        try {
            List<HandwritingParseResult> handwritingResults = parseHandwritingTcpPackets(handwritingData);
            //System.out.println("手写数据解析结果:");
            for (HandwritingParseResult result : handwritingResults) {
                //System.out.println(result);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("手写数据解析失败：" + e.getMessage());
        }

        // 测试按键数据解析
        byte[] buttonData = {0x55, 0x56, 0x07, 0x02, 0x01, 0x00, 0x00, 0x00, 0x00, 0x01, 0x08};
        try {
            ButtonParseResult buttonResult = parseButtonTcpPackets(buttonData);
            //System.out.println("按键数据解析结果:");
            //System.out.println(buttonResult.toString());
            //System.out.println("按键状态:");
            //System.out.println("按键 " + buttonResult.getButton() + ": " + (buttonResult.isButtonState()? "按下" : "弹起"));

        } catch (IllegalArgumentException e) {
            System.err.println("按键数据解析失败：" + e.getMessage());
        }*/

        // 输入的十六进制字节数组：8B 10 3B 84 20 00 B0 4E 37 39 39 37
        byte[] byteArray = {
                (byte) 0x8B, (byte) 0x10, (byte) 0x3B, (byte) 0x84,
                (byte) 0x20, (byte) 0x00, (byte) 0xB0, (byte) 0x4E,
                (byte) 0x37, (byte) 0x39, (byte) 0x39, (byte) 0x37
        };

        // 使用ParseTcpDataUtil的byteArrayToInt方法转换
        int result = ParseTcpDataUtil.byteArrayToInt(byteArray);

        System.out.println("转换结果: " + result);
        System.out.println("十六进制表示: 0x" + Integer.toHexString(result));
        
        // 测试 hexStringToByteArray 方法
        String hexString = "8B103B842000B04E37393937";
        byte[] convertedArray = ParseTcpDataUtil.hexStringToByteArray(hexString);
        System.out.println("\n测试 hexStringToByteArray 方法:");
        System.out.println("输入十六进制字符串: " + hexString);
        System.out.println("转换后的字节数组: " + Arrays.toString(convertedArray));
        
        // 验证转换是否正确（反向转换）
        String reversedHexString = ParseTcpDataUtil.bytesToHexString(convertedArray);
        System.out.println("反向转换回十六进制字符串: " + reversedHexString);
        System.out.println("转换是否正确: " + hexString.equalsIgnoreCase(reversedHexString));
    }
    /**
     * 将byte数组转换为int
     * @param byteArray 字节数组
     * @return 转换后的整数
     */
    public static int byteArrayToInt(byte[] byteArray) {
        int number = 0;
        for (int i = 0; i < byteArray.length; i++) {
            number |= (byteArray[i] & 0xff) << (i * 8);
        }
        return number;
    }
    /**
     * 将int转换为6位byte数组
     * @param value 要转换的整数
     * @return 6位字节数组，低位在前
     */
    public static byte[] intToByteArray(int value) {
        byte[] byteArray = new byte[6];
        // 低字节在前
        byteArray[0] = (byte)(value & 0xFF);
        byteArray[1] = (byte)((value >> 8) & 0xFF);
        byteArray[2] = (byte)((value >> 16) & 0xFF);
        byteArray[3] = (byte)((value >> 24) & 0xFF);
        // 填充剩余的两位为0
        byteArray[4] = 0;
        byteArray[5] = 0;
        return byteArray;
    }
    
    /**
     * 对序列号进行编码，生成不重复的数字串
     * @param serialNumberBytes 序列号字节数组
     * @return 编码后的序列号数字串
     */
    public static String encodeSerialNumber(byte[] serialNumberBytes) {
        // 将字节数组转换为十六进制字符串
        String hexString = bytesToHexString(serialNumberBytes);
        
        // 将十六进制字符串转换为数字串
        // 每个十六进制字符对应一个或两个数字
        StringBuilder numberBuilder = new StringBuilder();
        for (char c : hexString.toCharArray()) {
            int value = Character.digit(c, 16);
            numberBuilder.append(value);
        }
        
        return numberBuilder.toString();
    }
    
    /**
     * 将字节数组转换为十六进制字符串
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    public static String bytesToHexString(byte[] bytes) {
        StringBuilder hexBuilder = new StringBuilder();
        for (byte b : bytes) {
            hexBuilder.append(String.format("%02X", b & 0xFF));
        }
        return hexBuilder.toString();
    }
    /**
     * 将十六进制字符串转换为字节数组
     * @param hexString 十六进制字符串
     * @return 字节数组
     */
    public static byte[] hexStringToByteArray(String hexString) {
        if (hexString == null || hexString.length() % 2 != 0) {
            throw new IllegalArgumentException("十六进制字符串长度必须为偶数");
        }
        byte[] byteArray = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i += 2) {
            byteArray[i / 2] = (byte) Integer.parseInt(hexString.substring(i, i + 2), 16);
        }
        return byteArray;
    }
    /**
     * 发送LCD显示数据到设备（C++函数void tep send(SOCKET s, char* p_data, uint8_t length)的Java翻译版本）
     * @param outputStream 输出流，用于发送数据
     * @param data 要发送的数据
     * @param length 数据长度
     * @throws IOException 发送异常
     */
    public static void sendLcdDisplayData(OutputStream outputStream, byte[] data, int length) throws IOException {
        // 创建缓冲区，最大259字节
        byte[] buff = new byte[259];
        // 设置包头
        buff[0] = 0x55;
        buff[1] = 0x56;
        // 设置长度字段：length + 1（数据长度+1）
        buff[2] = (byte)(length + 1);
        // 设置数据类型为0x04（LCD显示字符串）
        buff[3] = 0x04;
        // 复制数据到缓冲区
        System.arraycopy(data, 0, buff, 4, length);
        //System.out.println(buff.toString());
        // 计算并设置校验和
        int checksum = calculateChecksum(buff, 0, 4 + length - 1);
        buff[4 + length] = (byte)(checksum & 0xFF);
        //System.out.println("发向板子数据："+java.util.Arrays.toString(buff));
        // 发送整个数据包
        outputStream.write(buff, 0, length + 5);
        outputStream.flush();
    }

    /**
     * 发送LCD显示数据到设备（C++函数void tep send(SOCKET s, char* p_data, uint8_t length)的Java翻译版本）
     * @param outputStream 输出流，用于发送数据
     * @param data 要发送的数据
     * @param length 数据长度
     * @throws IOException 发送异常
     */
    public static void sendLcdDisplayDataBluetooth(OutputStream outputStream, byte[] data, int length,String mac) throws IOException {
        // 创建缓冲区，最大259字节
        byte[] buff = new byte[259];
        // 设置包头
        buff[0] = 0x55;
        buff[1] = 0x56;
        // 设置长度字段：length + 1（数据长度+1）
        buff[2] = (byte)(length + 1);
        // 设置数据类型为0x04（LCD显示字符串）
        buff[3] = 0x04;
        byte[] macByte=hexStringToByteArray(mac);
        // 复制数据到缓冲区
        System.arraycopy(macByte, 0, buff, 4, 6);
        System.arraycopy(data, 0, buff, 10, length);
        //System.out.println(buff.toString());
        // 计算并设置校验和
        int checksum = calculateChecksum(buff, 0, 4+ 6 + length - 1);
        buff[10 + length] = (byte)(checksum & 0xFF);
        //System.out.println("发向板子数据："+java.util.Arrays.toString(buff));
        // 发送整个数据包
        outputStream.write(buff, 0, length + 5 + 6);
        outputStream.flush();
    }

    public static List<HandwritingParseResult> parseHandwritingTcpPacketsBluetooth(byte[] data) {
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
        if (type != 0x81) {
            throw new IllegalArgumentException("数据类型异常，需为0x81");
        }

        // 验证总长度
        if (data.length != length + 4) { // Header(2) + Length(1) + Type(1) + MAC(6) + Packet(length-1-6) + Checksum(1) = length + 4
            throw new IllegalArgumentException("数据总长度与Length字段不匹配");
        }

        // 解析Checksum
        byte checksum = data[data.length - 1];
        int calculatedChecksum = calculateChecksum(data, 0, data.length - 2);
        boolean checksumValid = (calculatedChecksum & 0xFF) == (checksum & 0xFF);

        // 提取Packet数据
        byte[] packet = Arrays.copyOfRange(data, 10, data.length - 1);

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

    public static ButtonParseResult parseButtonTcpPacketsBluetooth(byte[] data) {
        // 基础合法性校验
        if (data == null || data.length < 10) {
            throw new IllegalArgumentException("数据长度异常，至少需为10字节");
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
        if (type != 0x82) {
            throw new IllegalArgumentException("数据类型异常，需为0x82");
        }

        // 验证总长度
        if (data.length != length + 4) {
            throw new IllegalArgumentException("数据总长度与Length字段不匹配");
        }

        // 解析Checksum
        byte checksum = data[data.length - 1];
        int calculatedChecksum = calculateChecksum(data, 0, data.length - 2);
        boolean checksumValid = (calculatedChecksum & 0xFF) == (checksum & 0xFF);

        // 提取Packet数据
        byte[] packet = Arrays.copyOfRange(data, 10, data.length - 1);

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
}