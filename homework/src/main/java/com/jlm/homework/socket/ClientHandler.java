package com.jlm.homework.socket;

import com.jlm.homework.entity.SmartDeviceUserRelation;
import com.jlm.homework.exception.ParameterNewException;
import com.jlm.homework.service.ISmartDeviceUserRelationService;
import com.jlm.homework.util.ParseTcpDataUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

// 客户端处理线程
public class ClientHandler implements Runnable {

    private final SimpMessagingTemplate messagingTemplate;
    private final ISmartDeviceUserRelationService smartDeviceUserRelationService;
    private final Socket clientSocket;

    public ClientHandler(Socket socket,SimpMessagingTemplate messagingTemplate,ISmartDeviceUserRelationService  smartDeviceUserRelationService) {
        this.clientSocket = socket;
        this.messagingTemplate = messagingTemplate;
        this.smartDeviceUserRelationService = smartDeviceUserRelationService;
    }

    @Override
    public void run() {
        try (
                // 使用InputStream和OutputStream直接处理字节数据
                InputStream in = clientSocket.getInputStream();
                OutputStream out = clientSocket.getOutputStream();
                PrintWriter writer = new PrintWriter(out, true);
        ) {
            String clientAddress = clientSocket.getInetAddress().getHostAddress();
            System.out.println("客户端连接: " + clientAddress);
            SmartDeviceUserRelation relation=smartDeviceUserRelationService.selectByIpAddress(clientAddress);

            // 持续处理客户端消息
            byte[] headerBuffer = new byte[4]; // 包头(2) + 长度(1) + 类型(1)
            int bytesRead;

            while (true) {
                // 首先读取包头和基本信息
                bytesRead = in.read(headerBuffer);
                if (bytesRead == -1) {
                    break; // 连接关闭
                }
                
                // 验证包头
                if (headerBuffer[0] != 0x55 || headerBuffer[1] != 0x56) {
                    System.err.println("无效的数据包格式：包头不匹配");
                    writer.println("无效的数据包格式：包头不匹配");
                    continue;
                }
                
                // 获取数据长度和类型
                int dataLength = headerBuffer[2] & 0xFF;
                byte dataType = headerBuffer[3];
                
                // 计算完整数据包长度
                int packetTotalLength = 2 + 1 + 1 + (dataLength - 1) + 1; // Header(2)+Length(1)+Type(1)+Packet(Length-1)+Checksum(1)
                
                // 创建完整数据包缓冲区
                byte[] fullPacketBuffer = new byte[packetTotalLength];
                
                // 复制已读取的头部数据
                System.arraycopy(headerBuffer, 0, fullPacketBuffer, 0, 4);
                
                // 读取剩余数据
                int remainingBytes = packetTotalLength - 4;
                int bytesReadSoFar = 4;
                
                while (remainingBytes > 0) {
                    int bytesReadNow = in.read(fullPacketBuffer, bytesReadSoFar, remainingBytes);
                    if (bytesReadNow == -1) {
                        throw new IOException("连接意外关闭");
                    }
                    bytesReadSoFar += bytesReadNow;
                    remainingBytes -= bytesReadNow;
                }
                
                System.out.println("收到 [" + clientAddress + "] TCP数据包: " + java.util.Arrays.toString(fullPacketBuffer));
                
                try {
                    // 根据数据类型进行解析
                    if (dataType == 0x01) { // 手写数据
                        List<HandwritingParseResult> results = ParseTcpDataUtil.parseHandwritingTcpPackets(fullPacketBuffer);
                        System.out.println("手写数据解析结果数量：" + results.size());
                        for (HandwritingParseResult result : results) {
                            System.out.println("数据包解析结果：" + result.toString());
                            if(relation!=null){
                                result.setUserId(relation.getUserId());
                            }
                            // 发送解析结果给客户端
                            writer.println("服务器回复: " + result.toString());
                            
                            // 通过WebSocket发送解析结果给前端
                            messagingTemplate.convertAndSend("/topic/writingData", result);
                        }
                    } else if (dataType == 0x02) { // 按键数据
                        ButtonParseResult result = ParseTcpDataUtil.parseButtonTcpPackets(fullPacketBuffer);
                        System.out.println("按键数据解析结果数量：" + result.toString());
                        if(relation!=null){
                            ClassroomResult classroomResult=new ClassroomResult();
                            classroomResult.setStudentId(relation.getUserId());

                            if(16==result.getButton()){//按键A
                                classroomResult.setOption("A");
                            }
                            if(64==result.getButton()){//按键B
                                classroomResult.setOption("B");
                            }
                            if(256==result.getButton()){//按键C
                                classroomResult.setOption("C");
                            }
                            if(32==result.getButton()){//按键D
                                classroomResult.setOption("D");
                            }

                            if(128==result.getButton()){//按键E
                                classroomResult.setOption("E");
                            }
                            if(512==result.getButton()){//按键F  签到
                                if(StringUtils.isNotEmpty(relation.getUserId())) {
                                    messagingTemplate.convertAndSend("/topic/studentSign", relation.getUserId());
                                }
                            }
                            if(StringUtils.isNotEmpty(classroomResult.getOption())) {
                                // 通过WebSocket发送解析结果给前端
                                messagingTemplate.convertAndSend("/topic/classroomData", classroomResult);
                            }

                            if(8==result.getButton()){//确定

                            }
                            if(2==result.getButton()){//返回

                            }
                        }

                    } else if (dataType == 0x03) { // 序列号数据
                        MacParseResult result = ParseTcpDataUtil.parseSerialNumberTcpPacket(fullPacketBuffer);
                        System.out.println("序列号数据解析结果：" + result.toString());
                        
                        // 处理序列号关联
                        SmartDeviceUserRelation deviceUserRelation = smartDeviceUserRelationService.selectByDeviceCode(result.getMac().toString());
                        if (deviceUserRelation != null) {
                            deviceUserRelation.setIpAddress(clientAddress);
                            smartDeviceUserRelationService.update(deviceUserRelation);
                        }else {
                            System.out.println(result.getMac()+"设备还未绑定学生，请检查！");
                            // 设备绑定学生
                            messagingTemplate.convertAndSend("/topic/bindStudent", result.getMac());
                        }
                    }
                    
                    // 回显接收到的数据
                    out.write(fullPacketBuffer);
                    out.flush();
                    
                } catch (IllegalArgumentException e) {
                    System.err.println("解析失败：" + e.getMessage());
                    writer.println("解析失败：" + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("客户端处理异常: " + e.getMessage());
        } finally {
            // 确保关闭客户端连接
            try {
                clientSocket.close();
                System.out.println("客户端断开: " + clientSocket.getInetAddress());
            } catch (IOException e) {
                System.err.println("关闭连接时出错: " + e.getMessage());
            }
        }
    }
}
