package com.jlm.homework.socket;

import com.alibaba.fastjson.JSONObject;
import com.jlm.homework.dto.HomeWork2Board;
import com.jlm.homework.entity.SmartDeviceUserRelation;
import com.jlm.homework.entity.StudentsWriteRecord;
import com.jlm.homework.service.ISmartDeviceUserRelationService;
import com.jlm.homework.service.IStudentsHomeworkNewService;
import com.jlm.homework.socket.boardmenu.MenuItemT;
import com.jlm.homework.socket.boardmenu.MenuT;
import com.jlm.homework.util.ParseTcpDataUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

// 客户端处理线程
public class ClientHandler implements Runnable {
    private InetSocketAddress realRemoteAddress;
    private boolean headerParsed = false;

    public static MenuT pCurrentMenu = null;
    public static boolean homeworkflag = false;
    public static List<HomeWork2Board> work2Boards = new ArrayList<>();
    public static MenuT mainMenu = null;
    List<StudentsWriteRecord> studentsWriteRecords =new ArrayList<>();


    private final SimpMessagingTemplate messagingTemplate;
    private final ISmartDeviceUserRelationService smartDeviceUserRelationService;
    private IStudentsHomeworkNewService studentsHomeworkNewService;
    private final Socket clientSocket;

    public ClientHandler(Socket socket,SimpMessagingTemplate messagingTemplate,ISmartDeviceUserRelationService  smartDeviceUserRelationService,IStudentsHomeworkNewService studentsHomeworkNewService) {
        this.clientSocket = socket;
        this.messagingTemplate = messagingTemplate;
        this.smartDeviceUserRelationService = smartDeviceUserRelationService;
        this.studentsHomeworkNewService = studentsHomeworkNewService;
    }

    @Override
    public void run() {
        try (
                // 使用InputStream和OutputStream直接处理字节数据
                InputStream in = clientSocket.getInputStream();
                OutputStream out = clientSocket.getOutputStream();
                PrintWriter writer = new PrintWriter(out, true);

        ) {
            this.readProxyHeader();
            // 获取客户端真实IP
            InetSocketAddress realAddress = this.getRealRemoteAddress();
            String clientIP = realAddress.getHostString();
            int clientPort = realAddress.getPort();
            System.out.println("客户端真实IP:"+clientIP +" 端口号："+clientPort);

            SmartDeviceUserRelation relation=smartDeviceUserRelationService.selectByIpAddress(clientIP);

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
                
                System.out.println("收到 [" + clientIP + "] TCP数据包: " + java.util.Arrays.toString(fullPacketBuffer));
                
                try {


                    // 根据数据类型进行解析
                    if (dataType == 0x01) { // 手写数据
                        List<HandwritingParseResult> results = ParseTcpDataUtil.parseHandwritingTcpPackets(fullPacketBuffer);
                        System.out.println("手写数据解析结果数量：" + results.size());

                        for (HandwritingParseResult result : results) {
                            System.out.println("数据包解析结果：" + result.toString());
                            if (relation != null) {
                                if(homeworkflag&&pCurrentMenu!=null){//作业
                                    System.out.println("=============作业数据发送====");
                                    StudentsWriteRecord writeRecord = new StudentsWriteRecord();
                                    writeRecord.setX(result.getX());
                                    writeRecord.setY(result.getY());
                                    writeRecord.setPressure(result.getPressure());
                                    writeRecord.setTimestamp(result.getTimestamp());
                                    studentsWriteRecords.add(writeRecord);
                                }else {//课堂
                                    System.out.println("=============课堂数据发送====");
                                    result.setUserId(relation.getUserId());
                                    // 发送解析结果给客户端
                                    writer.println("服务器回复: " + result.toString());
                                    // 通过WebSocket发送解析结果给前端
                                    messagingTemplate.convertAndSend("/topic/writingData", result);
                                }
                            }

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
                                if(homeworkflag){
                                    System.out.println("=============作业数据发送====");
                                    if(pCurrentMenu.equals(mainMenu)){//
                                        if(work2Boards!=null&&work2Boards.size()>0){
                                            List<MenuItemT> itemTList = new ArrayList<>();
                                            int nb = 1;
                                            for(HomeWork2Board  board :work2Boards){
                                                String name = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem()).getDesc();
                                                if(board.getSubject().equals(name)){
                                                    String homeworkName = board.getHomeworkName();
                                                    studentsHomeworkNewService.saveStartTime(board.getHomeworkId());
                                                    if((board.getPageSize()!=null&&board.getPageSize()>0)){
                                                        for(int i=0;i<board.getPageSize();i++){
                                                            String desc = homeworkName.length()>6?homeworkName.substring(0,6):homeworkName+" " +(i+1);
                                                            MenuItemT menuItemT = new MenuItemT(nb,board.getHomeworkId(),desc,null);
                                                            itemTList.add(menuItemT);
                                                        }
                                                    }else{
                                                        String desc = homeworkName.length()>6?homeworkName.substring(0,6):homeworkName+" 1";
                                                        MenuItemT menuItemT = new MenuItemT(nb,board.getHomeworkId(),desc,null);
                                                        itemTList.add(menuItemT);
                                                    }

                                                }
                                            }
                                            MenuT menuT = new MenuT(pCurrentMenu,itemTList,0,0,3<itemTList.size()?3:itemTList.size(),itemTList.size());
                                            pCurrentMenu = menuT;
                                            pCurrentMenu.setShowStartItem(0);
                                            pCurrentMenu.setShowEndItem(itemTList.size());
                                            pCurrentMenu.setSelectItem(0);
                                            nMenuUpdate(out,writer);
                                        }

                                    }else {
                                        //保存作业记录
                                        if (studentsWriteRecords.size() > 0 && pCurrentMenu != null  && relation != null) {
                                            MenuItemT itemT = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem());
                                            String name = itemT.getDesc();
                                            Long homeworkId = itemT.getObjectId();
                                            Integer pageN = 1;
                                            String homeworkName;
                                            if(name.contains(" ")) {
                                                int num = name.lastIndexOf(" ");
                                                homeworkName = name.substring(0, num);

                                                pageN = Integer.valueOf(name.substring(num+1, name.length()));
                                            }else{
                                                homeworkName = name;
                                                pageN = 1;
                                            }
                                            studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()), homeworkId,homeworkName, pageN, studentsWriteRecords,true);
                                            pCurrentMenu = null;
                                            work2Boards = new ArrayList<>();
                                            mainMenu = null;
                                            homeworkflag = false;
                                            nMenuUpdate(out, writer);
                                        }
                                    }
                                }else if(StringUtils.isNotEmpty(relation.getUserId())) {
                                    System.out.println("=============课堂数据发送====");
                                    messagingTemplate.convertAndSend("/topic/endWrite", relation.getUserId());
                                }

                            }
                            if(1==result.getButton()){//菜单
                                homeworkflag = true;
                                System.out.println("++++++++++++菜单++++++++++");
                                if (relation != null) {
                                    Long studentId = Long.parseLong(relation.getUserId());
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                                    String day = sdf.format(new Date());
                                    work2Boards = studentsHomeworkNewService.getHomeWork2Board(null, day, studentId);
                                }
                                List<MenuItemT> mainItems = new ArrayList<>();
                                if(work2Boards==null||work2Boards.size()<=0){
                                    mainItems.add(new MenuItemT(1,null," 语文", null));
                                    mainItems.add(new MenuItemT(2,null," 数学", null));
                                    mainItems.add(new MenuItemT(3,null," 英语", null));
                                    mainItems.add(new MenuItemT(4,null," 科学", null));
                                    mainItems.add(new MenuItemT(5,null," 美术", null));
                                }else {
                                    Map<String,List<MenuItemT>> subjectMap = new HashMap<>();
                                    Map<String,MenuItemT> menuItemTMap = new HashMap<>();
                                    for (int i = 0; i < work2Boards.size(); i++) {
                                        HomeWork2Board homeWork2Board = work2Boards.get(i);
                                        String subject = homeWork2Board.getSubject().trim();
                                        if (StringUtils.isNotEmpty(subject)) {

                                            List<MenuItemT> pItems = null;
                                            MenuItemT itemT = null;
                                            if(subjectMap.containsKey(subject)){
                                                pItems = subjectMap.get(subject);
                                                itemT = menuItemTMap.get(subject);
                                            }else{
                                                itemT = new MenuItemT(i + 1,null, subject, null);
                                                pItems = new ArrayList<>();
                                                menuItemTMap.put(subject, itemT);

                                            }
                                            /*if (homeWork2Board.getPageSize() > 0) {
                                                for (int j = 1; j <= homeWork2Board.getPageSize(); j++) {
                                                    String name = homeWork2Board.getHomeworkName() + "  第" + j + "页 \n";
                                                    MenuItemT subItem = new MenuItemT(j, name, null);
                                                    pItems.add(subItem);
                                                }
                                            }*/

                                            subjectMap.put(subject,pItems);
                                        }

                                    }
                                    for(String subject: menuItemTMap.keySet()){
                                        MenuT subMenuT = new MenuT(mainMenu, subjectMap.get(subject), 0, 0, subjectMap.get(subject).size(), subjectMap.get(subject).size());
                                        MenuItemT itemT=menuItemTMap.get(subject);
                                        itemT.setPSubMenu(subMenuT);
                                        mainItems.add(itemT);

                                    }
                                }

                                mainMenu = new MenuT(null,mainItems,0,0,mainItems.size(),mainItems.size());
                                for(MenuItemT subItem:mainMenu.getPItems()){
                                    if(subItem!=null){
                                        subItem.setPSubMenu(mainMenu);
                                    }
                                }
                                pCurrentMenu = mainMenu;
                                pCurrentMenu.setShowStartItem(0);
                                pCurrentMenu.setShowEndItem(mainItems.size());
                                pCurrentMenu.setSelectItem(0);
                                nMenuUpdate(out,writer);
                            }
                            if(2==result.getButton()){//返回
                                if(pCurrentMenu!=null){
                                    if(pCurrentMenu.getParentMenu()!=null){
                                        pCurrentMenu = pCurrentMenu.getParentMenu();
                                        nMenuUpdate(out, writer);
                                    }else{
                                        pCurrentMenu = null;
                                        homeworkflag = false;
                                        work2Boards =new ArrayList<>();
                                        mainMenu = null;
                                        nMenuUpdate(out, writer);
                                    }
                                }else{
                                    pCurrentMenu = null;
                                    homeworkflag = false;
                                    work2Boards =new ArrayList<>();
                                    mainMenu = null;
                                    nMenuUpdate(out, writer);
                                }
                                if(homeworkflag){
                                    //保存作业记录
                                    if(studentsWriteRecords.size()>0&&pCurrentMenu!=null&&pCurrentMenu.getParentMenu()!=null&&relation!=null){
                                        MenuItemT itemT = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem());
                                        String name = itemT.getDesc();
                                        Long homeworkId = itemT.getObjectId();
                                        Integer pageN = 1;
                                        String homeworkName;
                                        if(name.contains(" ")) {
                                            int num = name.lastIndexOf(" ");
                                            homeworkName = name.substring(0, num);

                                            pageN = Integer.valueOf(name.substring(num+1, name.length()));
                                        }else{
                                            homeworkName = name;
                                            pageN = 1;
                                        }
                                        studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()),homeworkId,homeworkName,pageN,studentsWriteRecords,false);
                                    }
                                }
                            }
                            if(4==result.getButton()){//清除

                                if(homeworkflag){
                                    //保存作业记录
                                    if(studentsWriteRecords.size()>0&&pCurrentMenu!=null&&pCurrentMenu.getParentMenu()!=null&&relation!=null){
                                        MenuItemT itemT = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem());
                                        String name = itemT.getDesc();
                                        Long homeworkId = itemT.getObjectId();
                                        Integer pageN=1;
                                        String homeworkName;
                                        if(name.contains(" ")) {
                                            int num = name.lastIndexOf(" ");
                                            homeworkName = name.substring(0, num);

                                            pageN = Integer.valueOf(name.substring(num+1, name.length()));
                                        }else{
                                            homeworkName = name;
                                            pageN = 1;
                                        }
                                        studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()),homeworkId,homeworkName,pageN,studentsWriteRecords,false);
                                    }
                                    pCurrentMenu = null;
                                    homeworkflag = false;
                                    work2Boards =new ArrayList<>();
                                    mainMenu = null;
                                    nMenuUpdate(out, writer);
                                }else if (StringUtils.isNotEmpty(relation.getUserId())) {
                                    messagingTemplate.convertAndSend("/topic/clean", relation.getUserId());
                                }
                            }
                            if(1024==result.getButton()){//上一页

                                System.out.println("++++++++++++上一页++++++++++");
                                if(homeworkflag) {
                                    //保存作业记录
                                    if(studentsWriteRecords.size()>0&&pCurrentMenu!=null&&pCurrentMenu.getParentMenu()!=null&&relation!=null){
                                        int index = pCurrentMenu.getSelectItem();
                                        MenuItemT itemT = pCurrentMenu.getPItems().get(index);
                                        String name = itemT.getDesc();
                                        Long homeworkId = itemT.getObjectId();
                                        Integer pageN = 1;
                                        String homeworkName;
                                        if(name.contains(" ")) {
                                            int num = name.lastIndexOf(" ");
                                            homeworkName = name.substring(0, num);

                                            pageN = Integer.valueOf(name.substring(num+1, name.length()));
                                        }else{
                                            homeworkName = name;
                                            pageN = 1;
                                        }
                                        studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()),homeworkId,homeworkName,pageN,studentsWriteRecords,false);
                                    }
                                    if (pCurrentMenu.getSelectItem() > pCurrentMenu.getShowStartItem()) {
                                        Integer selectItem = pCurrentMenu.getSelectItem();
                                        selectItem = selectItem - 1;
                                        pCurrentMenu.setSelectItem(selectItem);
                                        nMenuUpdate(out, writer);
                                    } else {
                                        if (pCurrentMenu.getSelectItem() > 0) {
                                            pCurrentMenu.setShowStartItem(pCurrentMenu.getSelectItem() - 1);
                                            pCurrentMenu.setShowEndItem(pCurrentMenu.getShowEndItem() - 1);
                                            pCurrentMenu.setSelectItem(pCurrentMenu.getSelectItem() - 1);
                                            nMenuUpdate(out, writer);
                                        }
                                    }

                                }else{
                                    messagingTemplate.convertAndSend("/topic/lastPage", relation.getUserId());
                                }
                            }
                            if(2048==result.getButton()){//下一页

                                System.out.println("++++++++++++下一页++++++++++");
                                if(homeworkflag) {
                                    //保存作业记录
                                    if(studentsWriteRecords.size()>0&&pCurrentMenu!=null&&pCurrentMenu.getParentMenu()!=null&&relation!=null){
                                        int index = pCurrentMenu.getSelectItem();

                                        MenuItemT itemT = pCurrentMenu.getPItems().get(index);
                                        String name = itemT.getDesc();
                                        Long homeworkId = itemT.getObjectId();
                                        Integer pageN = 1;
                                        String homeworkName;
                                        if(name.contains(" ")) {
                                            int num = name.lastIndexOf(" ");
                                            homeworkName = name.substring(0, num);

                                            pageN = Integer.valueOf(name.substring(num+1, name.length()));
                                        }else{
                                            homeworkName = name;
                                            pageN = 1;
                                        }
                                        studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()),homeworkId,homeworkName,pageN,studentsWriteRecords,false);
                                    }
                                    if (pCurrentMenu.getSelectItem() < pCurrentMenu.getShowEndItem()) {
                                        Integer selectItem = pCurrentMenu.getSelectItem();
                                        selectItem = selectItem + 1;
                                        pCurrentMenu.setSelectItem(selectItem);
                                        nMenuUpdate(out, writer);
                                    } else {
                                        if (pCurrentMenu.getSelectItem() < pCurrentMenu.getMaxItems() - 1) {
                                            pCurrentMenu.setShowStartItem(pCurrentMenu.getSelectItem() + 1);
                                            pCurrentMenu.setShowEndItem(pCurrentMenu.getShowEndItem() + 1);
                                            pCurrentMenu.setSelectItem(pCurrentMenu.getSelectItem() + 1);
                                            nMenuUpdate(out, writer);
                                        }
                                    }

                                }else{
                                    messagingTemplate.convertAndSend("/topic/nextPage", relation.getUserId());
                                }
                            }

                            //按键松开
                            if(0==result.getButton()){
                                System.out.println("++++++++++++按键松开++++++++++");

                            }
                        }

                    } else if (dataType == 0x03) { // 序列号数据
                        MacParseResult result = ParseTcpDataUtil.parseSerialNumberTcpPacket(fullPacketBuffer);
                        System.out.println("序列号数据解析结果：" + result.toString());
                        
                        // 处理序列号关联
                        SmartDeviceUserRelation deviceUserRelation = smartDeviceUserRelationService.selectByDeviceCode(result.getMac().toString());
                        if (deviceUserRelation != null) {
                            deviceUserRelation.setIpAddress(clientIP);
                            smartDeviceUserRelationService.update(deviceUserRelation);
                        }else {
                            System.out.println(result.getMac()+"设备还未绑定学生，请检查！");
                            // 设备绑定学生
                            deviceUserRelation = new SmartDeviceUserRelation();
                            deviceUserRelation.setIpAddress(clientIP);
                            deviceUserRelation.setDeviceCode(result.getMac().toString());
                            messagingTemplate.convertAndSend("/topic/bindStudent", deviceUserRelation);
                        }
                    }else if (dataType == 0x04) { // 屏幕显示
                        try {
                            LCDDisplayParseResult result = ParseTcpDataUtil.parseLcdDisplayTcpPacket(fullPacketBuffer);
                            System.out.println("屏幕显示数据解析结果：" + result.toString());
                            String displayString = result.getDisplayString();

                            // 这里可以添加对显示字符串的处理逻辑
                            // 例如，如果需要查询相关作业数据
                            if (relation != null) {
                                Long studentId = Long.parseLong(relation.getUserId());
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                                String day = sdf.format(new Date());
                                List<HomeWork2Board> work2Board = studentsHomeworkNewService.getHomeWork2Board(displayString, day, studentId);
                                String jsonStr = JSONObject.toJSONString(work2Board);
                                out.write(jsonStr.getBytes());
                            }
                        } catch (UnsupportedEncodingException e) {
                            System.err.println("解析屏幕显示数据失败：" + e.getMessage());
                            writer.println("解析屏幕显示数据失败：" + e.getMessage());
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


    /**
     * 更新菜单并发送到客户端
     * @param out 输出流，用于发送数据
     * @throws IOException IO异常
     */
    private  void nMenuUpdate(OutputStream out, PrintWriter writer) throws IOException {
        // 对应C++的char buff[128] = {0}
        byte[] buff = new byte[128];
        int len = 0; // 对应C++的uint8_t len = 0
        System.out.println("__________________+++++输出屏幕开始++++++++___________________");
        // 获取当前菜单
        MenuT pCurrentMenu = ClientHandler.pCurrentMenu;
        //System.out.println("菜单内容个数:"+pCurrentMenu.getPItems().size());
        if (pCurrentMenu != null) {
            // 对应C++的for循环
            for (int i = pCurrentMenu.getShowStartItem(); i < pCurrentMenu.getShowEndItem(); i++) {
                // 获取菜单项
                if(pCurrentMenu.getPItems()==null||pCurrentMenu.getPItems().size()<=0){
                    break;
                }
                MenuItemT menuItem = pCurrentMenu.getPItems().get(i);
                // 获取描述并转换为UTF-16LE编码字节数组
                byte[] desc = menuItem.getDesc2Byte();
                System.out.println("_______"+desc.toString());
                byte[] descBytes = desc;//.getBytes(StandardCharsets.UTF_16LE);

                // 对应C++的memcpy
                System.arraycopy(desc, 0, buff, len, desc.length);

                // 检查是否是选中项
                if (pCurrentMenu.getSelectItem() == i) {
                    len += desc.length;
                    // 添加特殊标记 0x92 0x21
                    if (len < buff.length - 1) {
                        buff[len] = (byte) 0x92;
                        buff[len + 1] = (byte) 0x21;
                        len += 2;
                    }
                } else {
                    // 设置默认值
                    buff[0] = 0x00;
                    buff[1] = 0x30;
                    len += 2;
                }
            }
        } else {
            // 没有当前菜单时的默认值
            buff[0] = 0x00;
            buff[1] = 0x30;
            len = 2;
        }
        System.out.println("+++++++++"+out.toString()+",长度:"+len);
        // 创建长度为len的子数组并发送
        // 对应C++的tcp_send(sl, buff, len)
        if (len > 0 && out != null) {
            byte[] sendData = new byte[len];
            System.arraycopy(buff, 0, sendData, 0, len);
            System.out.println(new String(sendData, StandardCharsets.UTF_16LE));
            ParseTcpDataUtil.sendLcdDisplayData(out,sendData,len);
            out.flush();
        }
    }



    /**
     * 解析 PROXY Protocol 头部
     * 必须在读取业务数据前调用
     */
    public void readProxyHeader() throws IOException {
        if (headerParsed) return;

        PushbackInputStream pb = new PushbackInputStream(clientSocket.getInputStream(), 108);
        byte[] signature = new byte[5];
        int bytesRead = pb.read(signature);

        if (bytesRead != 5) {
            throw new IOException("读取协议签名失败");
        }

        pb.unread(signature);

        System.out.println(signature.toString()+",签名标识：" +new String(signature));
        // 识别协议版本
        if (new String(signature).equals("PROXY")) {
            parseV1(pb);
        } else if (isV2Signature(signature)) {
            parseV2(pb);
        } else {
            // 无 PROXY Protocol，使用原始地址
            realRemoteAddress = (InetSocketAddress) clientSocket.getRemoteSocketAddress();
        }

        headerParsed = true;
    }

    private boolean isV2Signature(byte[] sig) {
        return sig[0] == 0x0D && sig[1] == 0x0A &&
                sig[2] == 0x0D && sig[3] == 0x0A &&
                sig[4] == 0x00;
    }

    private void parseV1(PushbackInputStream pb) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(pb));
        String line = reader.readLine();

        if (line == null || !line.startsWith("PROXY")) {
            throw new IOException("无效的 PROXY Protocol v1 头部");
        }

        String[] parts = line.split(" ");
        if (parts.length < 6) {
            throw new IOException("PROXY Protocol v1 格式错误");
        }

        String clientIP = parts[2];
        int clientPort = Integer.parseInt(parts[4]);
        realRemoteAddress = new InetSocketAddress(clientIP, clientPort);
    }

    private void parseV2(PushbackInputStream pb) throws IOException {
        DataInputStream in = new DataInputStream(pb);

        // 跳过 12 字节固定头部
        in.skipBytes(12);

        // 读取 IPv4 地址（4字节）
        byte[] addressBytes = new byte[4];
        in.readFully(addressBytes);

        // 读取端口（2字节）
        int port = in.readUnsignedShort();

        // 构造 IP 地址
        String ip = String.format("%d.%d.%d.%d",
                addressBytes[0] & 0xff,
                addressBytes[1] & 0xff,
                addressBytes[2] & 0xff,
                addressBytes[3] & 0xff);

        realRemoteAddress = new InetSocketAddress(ip, port);
    }

    /**
     * 获取客户端真实地址
     * 必须先调用 readProxyHeader()
     */
    public InetSocketAddress getRealRemoteAddress() {
        if (!headerParsed) {
            throw new IllegalStateException("请先调用 readProxyHeader()");
        }
        return realRemoteAddress;
    }


}



