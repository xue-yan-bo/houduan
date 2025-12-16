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
import com.alibaba.cloud.commons.lang.StringUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// 客户端处理线程
public class ClientHandler implements Runnable {
    
    public static int save_size = 2000;
    public static Long homeId = null;
    public static Integer page_num = null;
    public static MenuT pCurrentMenu = null; //当前菜单
    public static boolean mrnuflag = false;
    public static boolean homeworkflag = false;
    public static boolean emendflag = false;
    public static boolean feedbackflag = false;
    public static boolean errorTitleflag = false;
    public static List<HomeWork2Board> work2Boards = new ArrayList<>();
    public static List<HomeWork2Board> emendBoards = new ArrayList<>();
    public static MenuT mainMenu = null; //主菜单
    public static MenuT homeworkMenu = null; //作业模式
    public static MenuT emendMenu = null; //订正模式
    public static MenuT feedbackMenu = null; //反馈模式
    public static MenuT errorTitleMenu = null; //错题上传
    public static int querenJishu = 0;
    public static boolean isBluetooth = false;
    public static Integer mac = null;
    private static Long buttonTimes=null;

    private InetSocketAddress realRemoteAddress;
    private boolean headerParsed = false;


    List<HandwritingParseResult> studentCalssRecords =new ArrayList<>();
    List<StudentsWriteRecord> studentsWriteRecords =new ArrayList<>();
    List<StudentsWriteRecord> studentsEmendRecords =new ArrayList<>();
    List<StudentsWriteRecord> studentsFeedbackRecords =new ArrayList<>();
    List<StudentsWriteRecord> uploadErrorTitleRecords =new ArrayList<>();
    List<StudentsWriteRecord> lastList = new ArrayList<>();

    private final SimpMessagingTemplate messagingTemplate;
    private final ISmartDeviceUserRelationService smartDeviceUserRelationService;
    private IStudentsHomeworkNewService studentsHomeworkNewService;
    private final Socket clientSocket;


    // 添加心跳相关成员变量
    private ScheduledExecutorService heartbeatScheduler;
    private static final byte HEARTBEAT_TYPE = 0x05; // 定义心跳包类型
    private static final long HEARTBEAT_INTERVAL = 9; // 心跳包间隔（秒）

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

            clientSocket.setKeepAlive(true);
            this.readProxyHeader();
            // 获取客户端真实IP
            InetSocketAddress realAddress = this.getRealRemoteAddress();
            String clientIP = realAddress.getHostString();
            int clientPort = realAddress.getPort();
            //System.out.println("客户端真实IP:"+clientIP +" 端口号："+clientPort);

            SmartDeviceUserRelation relation=smartDeviceUserRelationService.selectByIpAddress(clientIP);

            // 持续处理客户端消息
            byte[] headerBuffer = new byte[4]; // 包头(2) + 长度(1) + 类型(1)
            int bytesRead;
            // 初始化心跳包定时发送器
            initHeartbeatScheduler(out);
            while (true) {
                // 首先读取包头和基本信息
                bytesRead = in.read(headerBuffer);
                if (bytesRead == -1) {
                    break; // 连接关闭
                }
                
                // 验证包头
                if (headerBuffer[0] != 0x55 || headerBuffer[1] != 0x56) {
                    System.out.println("收到 [" + clientIP + "] TCP数据包: " + java.util.Arrays.toString(headerBuffer));
                    System.err.println("无效的数据包格式：包头不匹配");
                    writer.println("无效的数据包格式：包头不匹配");
                    continue;
                }
                
                // 获取数据长度和类型
                int dataLength = headerBuffer[2] & 0xFF;
                byte dataType = headerBuffer[3];
                if (dataType == 0x81 || dataType == 0x82) {
                    isBluetooth = true;
                    // 提取Packet数据（序列号）
                    byte[] packet = Arrays.copyOfRange(headerBuffer, 4, 10);
                    mac = ParseTcpDataUtil.byteArrayToInt(packet);
                    relation=smartDeviceUserRelationService.selectByDeviceCode(mac.toString());
                    if(relation==null){
                        System.out.println(mac+"设备还未绑定学生，请检查！");
                        // 设备绑定学生
                        SmartDeviceUserRelation deviceUserRelation = new SmartDeviceUserRelation();
                        deviceUserRelation.setIpAddress(clientIP);
                        deviceUserRelation.setDeviceCode(mac.toString());
                        messagingTemplate.convertAndSend("/topic/bindStudent", deviceUserRelation);
                    }else if(StringUtils.isEmpty(relation.getIpAddress())){
                        relation.setIpAddress(clientIP);
                        smartDeviceUserRelationService.update(relation);
                    }
                }else{
                    isBluetooth = false;
                }
                // 计算完整数据包长度
                int packetTotalLength = 2 + 1 + 1 + (dataLength - 1) + 1; // Header(2)+Length(1)+Type(1)+Packet(Length-1)+Checksum(1)
                if (dataType == 0x81 || dataType == 0x82){
                    packetTotalLength = 2 + 1 + 1 +6+ (dataLength - 1) + 1; // Header(2)+Length(1)+Type(1)+MAC(6)+Packet(Length-1)+Checksum(1)

                }
                // 创建完整数据包缓冲区
                byte[] fullPacketBuffer = new byte[packetTotalLength];
                
                // 复制已读取的头部数据
                if (dataType == 0x81 || dataType == 0x82){
                    System.arraycopy(headerBuffer, 0, fullPacketBuffer, 0, 10);
                }else {
                    System.arraycopy(headerBuffer, 0, fullPacketBuffer, 0, 4);
                }
                
                // 读取剩余数据
                int remainingBytes = packetTotalLength - 4;
                int bytesReadSoFar = 4;
                if (dataType == 0x81 || dataType == 0x82){
                    remainingBytes = packetTotalLength - 10;
                    bytesReadSoFar = 10;
                }
                while (remainingBytes > 0) {
                    int bytesReadNow = in.read(fullPacketBuffer, bytesReadSoFar, remainingBytes);
                    if (bytesReadNow == -1) {
                        throw new IOException("连接意外关闭");
                    }
                    bytesReadSoFar += bytesReadNow;
                    remainingBytes -= bytesReadNow;
                }
                
                //System.out.println("收到 [" + clientIP + "] TCP数据包: " + java.util.Arrays.toString(fullPacketBuffer));
                
                try {

                    // 根据数据类型进行解析
                    if (dataType == 0x01 || dataType == 0x81 ) { // 手写数据
                        List<HandwritingParseResult> results = null;
                        if(dataType == 0x01 ) {
                            results = ParseTcpDataUtil.parseHandwritingTcpPackets(fullPacketBuffer);
                        }else{
                            results = ParseTcpDataUtil.parseHandwritingTcpPacketsBluetooth(fullPacketBuffer);
                        }
                        System.out.println("手写数据解析结果数量：" + results.size());

                        for (HandwritingParseResult result : results) {
                            //System.out.println("数据包解析结果：" + result.toString());
                            if (relation != null) {
                                if(homeworkflag&&pCurrentMenu!=null){//作业
                                    //System.out.println("=============作业数据发送====");

                                    StudentsWriteRecord writeRecord = new StudentsWriteRecord();
                                    writeRecord.setX(result.getX());
                                    writeRecord.setY(result.getY());
                                    writeRecord.setPressure(result.getPressure());
                                    writeRecord.setTimestamp(result.getTimestamp());
                                    if(buttonTimes!=null&&result.getTimestamp()<buttonTimes){
                                        lastList.add(writeRecord);
                                    }else{
                                        studentsWriteRecords.add(writeRecord);
                                    }
                                    if(studentsWriteRecords.size()>=save_size&&homeId!=null&&page_num!=null){
                                        studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()), homeId,"1", page_num, studentsWriteRecords,false);
                                        studentsWriteRecords = new ArrayList<>();
                                    }
                                }else if(emendflag&&pCurrentMenu!=null){//订正
                                    //System.out.println("=============订正数据发送====");
                                    StudentsWriteRecord writeRecord = new StudentsWriteRecord();
                                    writeRecord.setX(result.getX());
                                    writeRecord.setY(result.getY());
                                    writeRecord.setPressure(result.getPressure());
                                    writeRecord.setTimestamp(result.getTimestamp());
                                    studentsEmendRecords.add(writeRecord);
                                    if(buttonTimes!=null&&result.getTimestamp()<buttonTimes){
                                        lastList.add(writeRecord);
                                    }else{
                                        studentsWriteRecords.add(writeRecord);
                                    }
                                    if(studentsEmendRecords.size()>=save_size&&homeId!=null&&page_num!=null){
                                        studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()), homeId,"2", page_num, studentsEmendRecords,false);
                                        studentsEmendRecords = new ArrayList<>();
                                    }
                                }else if(feedbackflag&&pCurrentMenu!=null){//反馈
                                    //System.out.println("=============反馈数据发送====");
                                    StudentsWriteRecord writeRecord = new StudentsWriteRecord();
                                    writeRecord.setX(result.getX());
                                    writeRecord.setY(result.getY());
                                    writeRecord.setPressure(result.getPressure());
                                    writeRecord.setTimestamp(result.getTimestamp());
                                    studentsFeedbackRecords.add(writeRecord);
                                }else if(errorTitleflag&&pCurrentMenu!=null){//错题
                                    //System.out.println("=============错题数据发送====");
                                    StudentsWriteRecord writeRecord = new StudentsWriteRecord();
                                    writeRecord.setX(result.getX());
                                    writeRecord.setY(result.getY());
                                    writeRecord.setPressure(result.getPressure());
                                    writeRecord.setTimestamp(result.getTimestamp());
                                    uploadErrorTitleRecords.add(writeRecord);
                                }else {//课堂
                                   // System.out.println("=============课堂数据发送====");
                                    result.setUserId(relation.getUserId());
                                    // 发送解析结果给客户端
                                    writer.println("服务器回复: " + result.toString());
                                    /*if(result.getPressure()>0){
                                        studentCalssRecords.add(result);
                                    }else{
                                        messagingTemplate.convertAndSend("/topic/writingData", studentCalssRecords);
                                        studentCalssRecords = new ArrayList<>();
                                    }*/
                                    // 通过WebSocket发送解析结果给前端
                                    messagingTemplate.convertAndSend("/topic/writingData/"+relation.getUserId(), result);
                                }
                            }else {

                                relation=smartDeviceUserRelationService.selectByIpAddress(clientIP);
                                if(relation!=null){
                                    result.setUserId(relation.getUserId());
                                    // 发送解析结果给客户端
                                    writer.println("服务器回复: " + result.toString());
                                    // 通过WebSocket发送解析结果给前端
                                    /*if(result.getPressure()>0){
                                        studentCalssRecords.add(result);
                                    }else{
                                        messagingTemplate.convertAndSend("/topic/writingData", studentCalssRecords);
                                        studentCalssRecords = new ArrayList<>();
                                    }*/
                                    messagingTemplate.convertAndSend("/topic/writingData/"+relation.getUserId(), result);
                                }else {
                                    System.out.println("=========没有获取到学生信息！======");
                                }

                            }

                        }

                    } else if (dataType == 0x02 || dataType == 0x82) { // 按键数据
                        ButtonParseResult result = null;
                        if(dataType == 0x02) {
                            result = ParseTcpDataUtil.parseButtonTcpPackets(fullPacketBuffer);
                        }else {
                            result = ParseTcpDataUtil.parseButtonTcpPacketsBluetooth(fullPacketBuffer);
                        }
                        //System.out.println("按键数据解析结果数量：" + result.toString());
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
                                //classroomResult.setOption("F");
                                if(StringUtils.isNotEmpty(relation.getUserId())) {
                                    messagingTemplate.convertAndSend("/topic/studentSign/"+relation.getUserId(), relation.getUserId());
                                }
                            }
                            if(StringUtils.isNotEmpty(classroomResult.getOption())) {
                                // 通过WebSocket发送解析结果给前端
                                messagingTemplate.convertAndSend("/topic/classroomData/"+relation.getUserId(), classroomResult);
                            }

                            if(8==result.getButton()){//确定
                                buttonTimes = null;
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                                if(homeworkflag){//末级菜单确认保存数据

                                    if(pCurrentMenu.equals(homeworkMenu)&&1==querenJishu){
                                        String name = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem()).getDesc().trim();
                                        //System.out.println("=============作业选择=====当前目录："+name);
                                        if(work2Boards==null||work2Boards.size()==0){
                                            Long studentId = Long.parseLong(relation.getUserId());
                                            String day = sdf.format(new Date());
                                            work2Boards = studentsHomeworkNewService.getHomeWork2Board(name, day, studentId);
                                        }
                                        if(work2Boards!=null&&work2Boards.size()>0){
                                            querenJishu=2;
                                            List<MenuItemT> itemTList = new ArrayList<>();
                                            int nb = 1;
                                            for(HomeWork2Board  board :work2Boards){

                                                if(StringUtils.isNotEmpty(board.getSubject())&&board.getSubject().trim().equals(name)){
                                                    String homeworkName = board.getHomeworkName();
                                                    if(board.getHomeworkId()!=null) {
                                                        studentsHomeworkNewService.saveStartTime(board.getHomeworkId());
                                                    }
                                                   // System.out.println("++++++++开始时间设定——————————");
                                                    //homeId = board.getHomeworkId();
                                                    //page_num = 1;
                                                    if((board.getPageSize()!=null&&board.getPageSize()>0)){
                                                        //System.out.println("++++++++作业名称："+homeworkName+"页个数" +board.getPageSize());
                                                        for(int i=0;i<board.getPageSize();i++){
                                                            String desc = homeworkName.length()>12?homeworkName.substring(0,11):homeworkName+" " +(i+1);
                                                            //System.out.println("++++++++菜单详情 "+desc);
                                                            MenuItemT menuItemT = new MenuItemT(nb,board.getHomeworkId(),desc,null);
                                                            itemTList.add(menuItemT);
                                                            nb++;
                                                        }
                                                    }else{
                                                        //System.out.println("++++++++作业名称："+homeworkName);
                                                        String desc = homeworkName.length()>12?homeworkName.substring(0,11):homeworkName+" 1";
                                                        //System.out.println("++++++++菜单详情 "+desc);
                                                        MenuItemT menuItemT = new MenuItemT(nb,board.getHomeworkId(),desc,null);
                                                        itemTList.add(menuItemT);
                                                        nb++;
                                                    }

                                                }
                                            }
                                            //System.out.println("++++++++菜单详情完成 ");
                                            MenuT menuT = new MenuT(homeworkMenu,itemTList,0,0,3<itemTList.size()?3:itemTList.size(),itemTList.size());
                                            pCurrentMenu = menuT;
                                            pCurrentMenu.setShowStartItem(0);
                                            pCurrentMenu.setShowEndItem(itemTList.size());
                                            pCurrentMenu.setSelectItem(0);
                                            nMenuUpdate(out,writer);
                                            if(pCurrentMenu!=null) {
                                                MenuItemT itemT = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem());
                                                if (itemT.getObjectId() != null) {
                                                    String name1 = itemT.getDesc();
                                                    homeId = itemT.getObjectId();
                                                    if (name1.contains(" ")) {
                                                        int num = name1.lastIndexOf(" ");
                                                        page_num = Integer.valueOf(name1.substring(num + 1, name1.length()));
                                                    } else {
                                                        page_num = 1;
                                                    }
                                                }
                                            }
                                        }else {
                                            //System.out.println("没有相关作业！");
                                            pCurrentMenu = null;
                                            work2Boards = new ArrayList<>();
                                            studentsWriteRecords =new ArrayList<>();
                                            homeworkflag = false;
                                            querenJishu=0;
                                            homeId = null;
                                            page_num = null;
                                            nMenuUpdate(out, writer);
                                        }

                                    }else {
                                        if(lastList!=null&&lastList.size()>0&&page_num>1){
                                            studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()), homeId,"1", page_num-1, lastList,false);
                                            lastList = new ArrayList<>();
                                        }
                                        //保存作业记录
                                        if (studentsWriteRecords.size() > 0 && pCurrentMenu != null  && relation != null) {
                                            //System.out.println("=============作业保存============");
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
                                            homeId = homeworkId;
                                            page_num = pageN;
                                            studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()), homeworkId,"1", pageN, studentsWriteRecords,true);
                                            pCurrentMenu = null;
                                            work2Boards = new ArrayList<>();
                                            studentsWriteRecords =new ArrayList<>();
                                            homeworkflag = false;
                                            querenJishu=0;
                                            homeId = null;
                                            page_num = null;
                                            nMenuUpdate(out, writer);
                                        }else{
                                            pCurrentMenu = null;
                                            work2Boards = new ArrayList<>();
                                            studentsWriteRecords =new ArrayList<>();
                                            homeworkflag = false;
                                            querenJishu=0;
                                            homeId = null;
                                            page_num = null;
                                            nMenuUpdate(out, writer);
                                        }
                                    }
                                }else if(emendflag){//末级菜单作业订正
                                    if(pCurrentMenu.equals(emendMenu)&&1==querenJishu){
                                        //System.out.println("=============订正作业选择====");
                                        querenJishu = 2;
                                        if(emendBoards!=null&&emendBoards.size()>0){
                                            List<MenuItemT> itemTList = new ArrayList<>();
                                            int nb = 1;
                                            for(HomeWork2Board  board :emendBoards){
                                                String name = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem()).getDesc();
                                                if(StringUtils.isNotEmpty(board.getSubject())&&board.getSubject().equals(name)){
                                                    String homeworkName = board.getHomeworkName();
                                                    //homeId = board.getHomeworkId();
                                                    //page_num = 1;
                                                    if((board.getPageSize()!=null&&board.getPageSize()>0)){
                                                        for(int i=0;i<board.getPageSize();i++){
                                                            String desc = homeworkName.length()>12?homeworkName.substring(0,11):homeworkName +" " +(i+1);
                                                            MenuItemT menuItemT = new MenuItemT(i+1,board.getHomeworkId(),desc,null);
                                                            itemTList.add(menuItemT);
                                                            nb++;
                                                        }
                                                    }else{
                                                        String desc = homeworkName.length()>12?homeworkName.substring(0,11):homeworkName+" 1";
                                                        MenuItemT menuItemT = new MenuItemT(nb,board.getHomeworkId(),desc,null);
                                                        itemTList.add(menuItemT);
                                                        nb++;
                                                    }

                                                }
                                            }
                                            MenuT menuT = new MenuT(emendMenu,itemTList,0,0,itemTList.size(),itemTList.size());
                                            pCurrentMenu = menuT;
                                            pCurrentMenu.setShowStartItem(0);
                                            pCurrentMenu.setShowEndItem(itemTList.size());
                                            pCurrentMenu.setSelectItem(0);
                                            nMenuUpdate(out,writer);
                                            if(pCurrentMenu!=null) {
                                                MenuItemT itemT = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem());
                                                if (itemT.getObjectId() != null) {
                                                    String name1 = itemT.getDesc();
                                                    homeId = itemT.getObjectId();
                                                    if (name1.contains(" ")) {
                                                        int num = name1.lastIndexOf(" ");
                                                        page_num = Integer.valueOf(name1.substring(num + 1, name1.length()));
                                                    } else {
                                                        page_num = 1;
                                                    }
                                                }
                                            }
                                        }else{
                                            //System.out.println("没有相关订正作业！");
                                            pCurrentMenu = null;
                                            work2Boards = new ArrayList<>();
                                            studentsEmendRecords =new ArrayList<>();
                                            homeworkflag = false;
                                            emendflag = false;
                                            errorTitleflag = false;
                                            querenJishu = 0;
                                            homeId = null;
                                            page_num = null;
                                            nMenuUpdate(out, writer);
                                        }
                                    }else {
                                        if(lastList!=null&&lastList.size()>0&&page_num>1){
                                            studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()), homeId,"2", page_num-1, lastList,false);
                                            lastList = new ArrayList<>();
                                        }
                                        //保存作业记录
                                        if (studentsEmendRecords.size() > 0 && pCurrentMenu != null  && relation != null) {
                                            //System.out.println("=============订正作业保存====");
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
                                            homeId = homeworkId;
                                            page_num = pageN;
                                            studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()), homeworkId,"2", pageN, studentsEmendRecords,true);
                                            pCurrentMenu = null;
                                            work2Boards = new ArrayList<>();
                                            studentsEmendRecords =new ArrayList<>();
                                            homeworkflag = false;
                                            emendflag = false;
                                            errorTitleflag = false;
                                            querenJishu = 0;
                                            homeId = null;
                                            page_num = null;
                                            nMenuUpdate(out, writer);
                                        }else{
                                            pCurrentMenu = null;
                                            work2Boards = new ArrayList<>();
                                            studentsEmendRecords =new ArrayList<>();
                                            homeworkflag = false;
                                            emendflag = false;
                                            errorTitleflag = false;
                                            querenJishu = 0;
                                            homeId = null;
                                            page_num = null;
                                            nMenuUpdate(out, writer);
                                        }
                                    }
                                }else if(feedbackflag) {//末级菜单反馈数据保存
                                    //System.out.println("=============保存反馈数据====");
                                    //保存反馈数据
                                    if (studentsFeedbackRecords.size() > 0 && pCurrentMenu != null  && relation != null) {
                                        MenuItemT itemT = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem());
                                        String name = itemT.getDesc();
                                        //System.out.println("=============保存反馈数据====科目："+name);
                                        studentsHomeworkNewService.saveFeedbackRecords(Long.parseLong(relation.getUserId()), name,studentsFeedbackRecords);
                                        //System.out.println("=============保存反馈数据完成====");
                                        pCurrentMenu = null;
                                        studentsFeedbackRecords = new ArrayList<>();
                                        homeworkflag = false;
                                        emendflag = false;
                                        feedbackflag = false;
                                        errorTitleflag = false;
                                        querenJishu = 0;
                                        nMenuUpdate(out, writer);
                                    }
                                }else if(errorTitleflag) {//末级菜单反馈数据保存
                                    //System.out.println("=============保存错题数据====");
                                    //保存错题数据
                                    if (uploadErrorTitleRecords.size() > 0 && pCurrentMenu != null  && relation != null) {
                                        MenuItemT itemT = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem());
                                        String name = itemT.getDesc();
                                        //System.out.println("=============保存反馈数据====科目："+name);
                                        studentsHomeworkNewService.saveErrorTitleRecords(Long.parseLong(relation.getUserId()), name,uploadErrorTitleRecords);
                                        //System.out.println("=============保存反馈数据完成====");
                                        pCurrentMenu = null;
                                        uploadErrorTitleRecords = new ArrayList<>();
                                        homeworkflag = false;
                                        emendflag = false;
                                        feedbackflag = false;
                                        errorTitleflag = false;
                                        querenJishu = 0;
                                        nMenuUpdate(out, writer);
                                    }
                                }else if(StringUtils.isNotEmpty(relation.getUserId())) {
                                    //System.out.println("=============课堂数据发送====");
                                    messagingTemplate.convertAndSend("/topic/endWrite/"+relation.getUserId(), relation.getUserId());
                                }
                                if(pCurrentMenu!=null){//模式确认选择

                                    String name = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem()).getDesc();
                                    if("作业模式".equals(name)) {
                                        homeworkflag = true;
                                        querenJishu = 1;
                                        //System.out.println("作业模式确认");
                                        if (relation != null) {
                                            Long studentId = Long.parseLong(relation.getUserId());

                                            String day = sdf.format(new Date());
                                            work2Boards = studentsHomeworkNewService.getHomeWork2Board(null, day, studentId);
                                        }

                                        if(work2Boards!=null&&work2Boards.size()>0){
                                            //System.out.println("===========作业数"+work2Boards.size());
                                            List<MenuItemT> homeworkItems = new ArrayList<>();
                                            Map<String,List<MenuItemT>> subjectMap = new HashMap<>();
                                            Map<String,MenuItemT> menuItemTMap = new HashMap<>();
                                            for (int i = 0; i < work2Boards.size(); i++) {
                                                HomeWork2Board homeWork2Board = work2Boards.get(i);
                                                //System.out.println("---------------作业："+homeWork2Board.getHomeworkName()+":"+homeWork2Board.getPageSize());
                                                if(StringUtils.isEmpty(homeWork2Board.getSubject())){
                                                    continue;
                                                }
                                                String subject = homeWork2Board.getSubject().trim();
                                                if (StringUtils.isNotEmpty(subject)) {

                                                    List<MenuItemT> pItems = null;
                                                    MenuItemT itemT = null;
                                                    if(subjectMap.containsKey(subject)&&menuItemTMap.containsKey(subject)){
                                                      //System.out.println("---------------科目："+subject);
                                                        pItems = subjectMap.get(subject);
                                                        itemT = menuItemTMap.get(subject);
                                                        //pItems.add(itemT);
                                                    }else{
                                                      //System.out.println("##############新建科目菜单："+subject);
                                                        itemT = new MenuItemT(i + 1,null, subject, null);
                                                        pItems = new ArrayList<>();
                                                        pItems.add(itemT);
                                                        menuItemTMap.put(subject, itemT);
                                                      //System.out.println("---------------科目菜单："+subject);
                                                    }
                                                  //System.out.println("=====科目下的作业数+++++++："+pItems.size());
                                                    subjectMap.put(subject,pItems);
                                                }
                                              //System.out.println("=====科目下的作业数："+subjectMap.size());
                                            }
                                          //System.out.println("科目菜单数："+menuItemTMap.size());
                                            for(String subject: menuItemTMap.keySet()){
                                              //System.out.println("作业科目："+subject);
                                                MenuT subMenuT = new MenuT(homeworkMenu, subjectMap.get(subject), 0, 0, subjectMap.get(subject).size(), subjectMap.get(subject).size());
                                                MenuItemT itemT=menuItemTMap.get(subject);
                                                itemT.setPSubMenu(subMenuT);
                                                homeworkItems.add(itemT);
                                            }
                                          //System.out.println("=============子菜单数："+homeworkItems.size());
                                            homeworkMenu = new MenuT(mainMenu,homeworkItems,0,0,homeworkItems.size(),homeworkItems.size());
                                            pCurrentMenu = homeworkMenu;
                                            pCurrentMenu.setShowStartItem(0);
                                            pCurrentMenu.setShowEndItem(homeworkItems.size());
                                            pCurrentMenu.setSelectItem(0);
                                            nMenuUpdate(out,writer);
                                        }else{
                                          //System.out.println("没有作业数");
                                            pCurrentMenu = null;
                                            work2Boards = new ArrayList<>();
                                            studentsWriteRecords =new ArrayList<>();
                                            homeworkflag = false;
                                            querenJishu=0;
                                            homeId = null;
                                            page_num = null;
                                            nMenuUpdate(out, writer);
                                        }


                                    }
                                    if("订正模式".equals(name)) {
                                        emendflag = true;
                                        querenJishu = 1;
                                        if (relation != null) {
                                            Long studentId = Long.parseLong(relation.getUserId());
                                            String day = sdf.format(new Date());
                                            emendBoards = studentsHomeworkNewService.getEmendHomeWork2Board(null, studentId);
                                        }
                                        if(emendBoards!=null&&emendBoards.size()>0){
                                            List<MenuItemT> emendItems = new ArrayList<>();
                                            Map<String,List<MenuItemT>> subjectMap = new HashMap<>();
                                            Map<String,MenuItemT> menuItemTMap = new HashMap<>();
                                            for (int i = 0; i < emendBoards.size(); i++) {
                                                HomeWork2Board homeWork2Board = emendBoards.get(i);
                                                String subject = homeWork2Board.getSubject().trim();
                                                if (StringUtils.isNotEmpty(subject)) {

                                                    List<MenuItemT> pItems = null;
                                                    MenuItemT itemT = null;
                                                    if(subjectMap.containsKey(subject)){
                                                        pItems = subjectMap.get(subject);
                                                        itemT = menuItemTMap.get(subject);
                                                        pItems.add(itemT);
                                                    }else{
                                                        itemT = new MenuItemT(i + 1,null, subject, null);
                                                        pItems = new ArrayList<>();
                                                        pItems.add(itemT);
                                                        menuItemTMap.put(subject, itemT);
                                                    }
                                                    subjectMap.put(subject,pItems);
                                                }

                                            }
                                            for(String subject: menuItemTMap.keySet()){
                                                MenuT subMenuT = new MenuT(emendMenu, subjectMap.get(subject), 0, 0, subjectMap.get(subject).size(), subjectMap.get(subject).size());
                                                MenuItemT itemT=menuItemTMap.get(subject);
                                                itemT.setPSubMenu(subMenuT);
                                                emendItems.add(itemT);
                                            }
                                            emendMenu = new MenuT(mainMenu,emendItems,0,0,emendItems.size(),emendItems.size());
                                            pCurrentMenu = emendMenu;
                                            pCurrentMenu.setShowStartItem(0);
                                            pCurrentMenu.setShowEndItem(emendItems.size());
                                            pCurrentMenu.setSelectItem(0);
                                            nMenuUpdate(out,writer);
                                        }else{
                                            pCurrentMenu = mainMenu;
                                            pCurrentMenu.setShowStartItem(0);
                                            pCurrentMenu.setShowEndItem(2);
                                            pCurrentMenu.setSelectItem(0);
                                            homeworkflag = false;
                                            emendflag = false;
                                            errorTitleflag = false;
                                            nMenuUpdate(out,writer);
                                        }
                                    }
                                    if("反馈模式".equals(name)){
                                        querenJishu = 1;
                                        List<MenuItemT> feedbackItems = new ArrayList<>();

                                        feedbackItems.add(new MenuItemT(1,null,"语文", null));
                                        feedbackItems.add(new MenuItemT(2,null,"数学", null));
                                        feedbackItems.add(new MenuItemT(3,null,"英语", null));
                                        feedbackItems.add(new MenuItemT(4,null,"历史", null));
                                        feedbackItems.add(new MenuItemT(5,null,"政治", null));
                                        feedbackMenu = new MenuT(null,feedbackItems,0,0,feedbackItems.size(),feedbackItems.size());
                                        pCurrentMenu = feedbackMenu;
                                        feedbackflag = true;
                                        pCurrentMenu.setShowStartItem(0);
                                        pCurrentMenu.setShowEndItem(feedbackItems.size());
                                        pCurrentMenu.setSelectItem(0);
                                        nMenuUpdate(out,writer);
                                    }
                                    if("错题上传".equals(name)){
                                        querenJishu = 1;
                                        List<MenuItemT> feedbackItems = new ArrayList<>();

                                        feedbackItems.add(new MenuItemT(1,null,"语文", null));
                                        feedbackItems.add(new MenuItemT(2,null,"数学", null));
                                        feedbackItems.add(new MenuItemT(3,null,"英语", null));
                                        feedbackItems.add(new MenuItemT(4,null,"历史", null));
                                        feedbackItems.add(new MenuItemT(5,null,"政治", null));
                                        errorTitleMenu = new MenuT(null,feedbackItems,0,0,feedbackItems.size(),feedbackItems.size());
                                        pCurrentMenu = errorTitleMenu;
                                        errorTitleflag = true;
                                        pCurrentMenu.setShowStartItem(0);
                                        pCurrentMenu.setShowEndItem(feedbackItems.size());
                                        pCurrentMenu.setSelectItem(0);
                                        nMenuUpdate(out,writer);
                                    }
                                }
                                mrnuflag = false;
                              //System.out.println("++++++++++++当前页数："+page_num+"++++++++");
                            }
                            if(1==result.getButton()){//菜单
                                mrnuflag = true;
                              //System.out.println("++++++++++++菜单++++++++++");
                                List<MenuItemT> mainItems = new ArrayList<>();

                                mainItems.add(new MenuItemT(1,null,"作业模式", null));
                                mainItems.add(new MenuItemT(2,null,"订正模式", null));
                                mainItems.add(new MenuItemT(3,null,"反馈模式", null));
                                mainItems.add(new MenuItemT(4,null,"错题上传", null));
                                mainMenu = new MenuT(null,mainItems,0,0,mainItems.size(),mainItems.size());
                                pCurrentMenu = mainMenu;
                                pCurrentMenu.setShowStartItem(0);
                                pCurrentMenu.setShowEndItem(mainItems.size());
                                pCurrentMenu.setSelectItem(0);
                                nMenuUpdate(out,writer);
                            }
                            if(2==result.getButton()){//返回
                                buttonTimes = null;
                                if(homeworkflag){
                                    if(lastList!=null&&lastList.size()>0&&page_num>1){
                                        studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()), homeId,"1", page_num-1, lastList,false);
                                        lastList = new ArrayList<>();
                                    }
                                    //保存作业记录
                                    if(studentsWriteRecords.size()>0&&pCurrentMenu!=null&&pCurrentMenu.getParentMenu()!=null&&relation!=null){
                                        MenuItemT itemT = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem());
                                        String name = itemT.getDesc();
                                        Long homeworkId = itemT.getObjectId();
                                        Integer pageN = 1;
                                        if(name.contains(" ")) {
                                            int num = name.lastIndexOf(" ");
                                            pageN = Integer.valueOf(name.substring(num+1, name.length()));
                                        }else{
                                            pageN = 1;
                                        }
                                        studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()),homeworkId,"1",pageN,studentsWriteRecords,false);
                                        studentsWriteRecords =new ArrayList<>();
                                    }
                                }
                                if(emendflag){
                                    if(lastList!=null&&lastList.size()>0&&homeId!=null&&page_num!=null&&page_num>1){
                                        studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()), homeId,"2", page_num-1, lastList,false);
                                        lastList = new ArrayList<>();
                                    }
                                    //保存作业记录
                                    if(studentsEmendRecords.size()>0&&pCurrentMenu!=null&&pCurrentMenu.getParentMenu()!=null&&relation!=null){
                                        MenuItemT itemT = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem());
                                        String name = itemT.getDesc();
                                        Long homeworkId = itemT.getObjectId();
                                        Integer pageN = 1;
                                        if(name.contains(" ")) {
                                            int num = name.lastIndexOf(" ");
                                            pageN = Integer.valueOf(name.substring(num+1, name.length()));
                                        }else{
                                            pageN = 1;
                                        }
                                        studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()),homeworkId,"2",pageN,studentsEmendRecords,false);
                                        studentsEmendRecords =new ArrayList<>();
                                    }
                                }
                                if(feedbackflag){
                                    //保存反馈数据
                                    if (studentsFeedbackRecords.size() > 0 && pCurrentMenu != null  && relation != null) {
                                        MenuItemT itemT = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem());
                                        String name = itemT.getDesc();
                                      //System.out.println("=============保存反馈数据====科目："+name);
                                        studentsHomeworkNewService.saveFeedbackRecords(Long.parseLong(relation.getUserId()), name,studentsFeedbackRecords);
                                      //System.out.println("=============保存反馈数据完成====");
                                        pCurrentMenu = null;
                                        studentsFeedbackRecords = new ArrayList<>();
                                        homeworkflag = false;
                                        emendflag = false;
                                        feedbackflag = false;
                                        errorTitleflag = false;
                                        nMenuUpdate(out, writer);
                                    }
                                }
                                if(pCurrentMenu!=null){
                                    if(pCurrentMenu.getParentMenu()!=null){
                                        pCurrentMenu = pCurrentMenu.getParentMenu();
                                        pCurrentMenu.setShowStartItem(0);
                                        pCurrentMenu.setShowEndItem(pCurrentMenu.getPItems().size());
                                        pCurrentMenu.setSelectItem(0);
                                        nMenuUpdate(out, writer);
                                        String name = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem()).getDesc().trim();
                                      //System.out.println("++++++++++++++++返回后目录选择为："+name);
                                        if(pCurrentMenu.equals(homeworkMenu)||pCurrentMenu.equals(emendMenu)){
                                            querenJishu = 1;
                                        }
                                    }else{
                                        pCurrentMenu = null;
                                        homeworkflag = false;
                                        emendflag = false;
                                        feedbackflag = false;
                                        errorTitleflag = false;
                                        studentsWriteRecords = new ArrayList<>();
                                        studentsEmendRecords = new ArrayList<>();
                                        studentsFeedbackRecords = new ArrayList<>();
                                        work2Boards =new ArrayList<>();
                                        homeId = null;
                                        page_num =null;
                                        nMenuUpdate(out, writer);
                                    }
                                }else{
                                    pCurrentMenu = null;
                                    homeworkflag = false;
                                    emendflag = false;
                                    errorTitleflag = false;
                                    work2Boards =new ArrayList<>();
                                    homeId = null;
                                    page_num =null;
                                    nMenuUpdate(out, writer);
                                }
                            }
                            if(4==result.getButton()){//清除
                                buttonTimes = null;
                                if(homeworkflag){
                                    if(lastList!=null&&lastList.size()>0&&homeId!=null&&page_num!=null&&page_num>1){
                                        studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()), homeId,"1", page_num-1, lastList,false);
                                        lastList = new ArrayList<>();
                                    }
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
                                        studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()),homeworkId,"1",pageN,studentsWriteRecords,false);
                                        studentsWriteRecords =new ArrayList<>();
                                    }

                                    pCurrentMenu = null;
                                    mrnuflag = false;
                                    homeworkflag = false;
                                    emendflag = false;
                                    errorTitleflag = false;
                                    studentsWriteRecords = new ArrayList<>();
                                    work2Boards =new ArrayList<>();
                                    homeworkMenu = null;
                                    emendMenu = null;
                                    homeId = null;
                                    page_num =null;
                                    nMenuUpdate(out, writer);
                                }else if(emendflag){
                                    if(lastList!=null&&lastList.size()>0&&homeId!=null&&page_num!=null&&page_num>1){
                                        studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()), homeId,"2", page_num-1, lastList,false);
                                        lastList = new ArrayList<>();
                                    }
                                    //保存作业记录
                                    if(studentsEmendRecords.size()>0&&pCurrentMenu!=null&&pCurrentMenu.getParentMenu()!=null&&relation!=null){
                                        MenuItemT itemT = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem());
                                        String name = itemT.getDesc();
                                        Long homeworkId = itemT.getObjectId();
                                        Integer pageN = 1;
                                        if(name.contains(" ")) {
                                            int num = name.lastIndexOf(" ");
                                            pageN = Integer.valueOf(name.substring(num+1, name.length()));
                                        }else{
                                            pageN = 1;
                                        }
                                        studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()),homeworkId,"2",pageN,studentsEmendRecords,false);
                                        studentsEmendRecords =new ArrayList<>();
                                    }
                                    pCurrentMenu = null;
                                    mrnuflag = false;
                                    homeworkflag = false;
                                    emendflag = false;
                                    errorTitleflag = false;
                                    studentsEmendRecords = new ArrayList<>();
                                    work2Boards =new ArrayList<>();
                                    homeworkMenu = null;
                                    emendMenu = null;
                                    homeId = null;
                                    page_num =null;
                                    nMenuUpdate(out, writer);
                                }else if(feedbackflag){
                                    studentsFeedbackRecords = new ArrayList<>();
                                    pCurrentMenu = null;
                                    homeworkflag = false;
                                    emendflag = false;
                                    feedbackflag = false;
                                    errorTitleflag = false;
                                    homeId = null;
                                    page_num =null;
                                    nMenuUpdate(out, writer);

                                }else if(mrnuflag){
                                    pCurrentMenu = null;
                                    mrnuflag = false;
                                    homeworkflag = false;
                                    emendflag = false;
                                    errorTitleflag = false;
                                    work2Boards =new ArrayList<>();
                                    homeworkMenu = null;
                                    emendMenu = null;
                                    nMenuUpdate(out, writer);
                                }else if (StringUtils.isNotEmpty(relation.getUserId())) {
                                    messagingTemplate.convertAndSend("/topic/clean/"+relation.getUserId(), relation.getUserId());
                                }
                            }
                            if(1024==result.getButton()){//上一页
                                buttonTimes = result.getTimestamp();
                              //System.out.println("++++++++++++上一页++++++++++");
                                if(homeworkflag) {
                                    if(lastList!=null&&lastList.size()>0&&homeId!=null&&page_num!=null){
                                        studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()), homeId,"1", page_num+1, lastList,false);
                                        lastList = new ArrayList<>();
                                    }
                                    //保存作业记录
                                    if(studentsWriteRecords.size()>0&&pCurrentMenu!=null&&relation!=null){
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
                                        studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()),homeworkId,"1",pageN,studentsWriteRecords,false);
                                        studentsWriteRecords = new ArrayList<>();
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

                                }else if(emendflag){
                                    if(lastList!=null&&lastList.size()>0&&homeId!=null&&page_num!=null){
                                        studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()), homeId,"2", page_num+1, lastList,false);
                                        lastList = new ArrayList<>();
                                    }
                                    //保存dindzhemg记录
                                    if(studentsEmendRecords.size()>0&&pCurrentMenu!=null&&relation!=null){
                                        MenuItemT itemT = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem());
                                        String name = itemT.getDesc();
                                        Long homeworkId = itemT.getObjectId();
                                        Integer pageN = 1;
                                        if(name.contains(" ")) {
                                            int num = name.lastIndexOf(" ");
                                            pageN = Integer.valueOf(name.substring(num+1, name.length()));
                                        }else{
                                            pageN = 1;
                                        }
                                        studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()),homeworkId,"2",pageN,studentsEmendRecords,false);
                                        studentsEmendRecords =new ArrayList<>();

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
                                }else if(feedbackflag){
                                    //保存反馈数据
                                    if (studentsFeedbackRecords.size() > 0 && pCurrentMenu != null  && relation != null) {
                                        MenuItemT itemT = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem());
                                        String name = itemT.getDesc();
                                      //System.out.println("=============保存反馈数据====科目："+name);
                                        studentsHomeworkNewService.saveFeedbackRecords(Long.parseLong(relation.getUserId()), name,studentsFeedbackRecords);
                                      //System.out.println("=============保存反馈数据完成====");

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
                                }else if(errorTitleflag){
                                    //保存反馈数据
                                    if (uploadErrorTitleRecords.size() > 0 && pCurrentMenu != null  && relation != null) {
                                        MenuItemT itemT = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem());
                                        String name = itemT.getDesc();
                                      //System.out.println("=============保存错题上传数据====科目："+name);
                                        studentsHomeworkNewService.saveErrorTitleRecords(Long.parseLong(relation.getUserId()), name,uploadErrorTitleRecords);
                                      //System.out.println("=============保存错题上传完成====");

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
                                }else if(mrnuflag){
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
                                    messagingTemplate.convertAndSend("/topic/lastPage/"+relation.getUserId(), relation.getUserId());
                                }
                                if(pCurrentMenu!=null) {
                                    MenuItemT itemT = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem());
                                    if (itemT.getObjectId() != null) {
                                        String name = itemT.getDesc();
                                        homeId = itemT.getObjectId();
                                        if (name.contains(" ")) {
                                            int num = name.lastIndexOf(" ");
                                            page_num = Integer.valueOf(name.substring(num + 1, name.length()));
                                        } else {
                                            page_num = 1;
                                        }
                                    }
                                }
                              //System.out.println("++++++++++++当前页数："+page_num+"++++++++");
                            }
                            if(2048==result.getButton()){//下一页
                                buttonTimes = result.getTimestamp();
                              //System.out.println("++++++++++++下一页++++++++++");
                                if(homeworkflag) {
                                    if(lastList!=null&&lastList.size()>0&&homeId!=null&&page_num!=null&&page_num>1){
                                        studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()), homeId,"1", page_num-1, lastList,false);
                                        lastList = new ArrayList<>();
                                    }
                                    String name = null;
                                    //保存作业记录
                                    if(studentsWriteRecords.size()>0&&pCurrentMenu!=null&&relation!=null){
                                        int index = pCurrentMenu.getSelectItem();

                                        MenuItemT itemT = pCurrentMenu.getPItems().get(index);
                                        name = itemT.getDesc();
                                        Long homeworkId = itemT.getObjectId();
                                        Integer pageN = 1;
                                        if(name.contains(" ")) {
                                            int num = name.lastIndexOf(" ");

                                            pageN = Integer.valueOf(name.substring(num+1, name.length()));
                                        }else{
                                            pageN = 1;
                                        }
                                        studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()),homeworkId,"1",pageN,studentsWriteRecords,false);
                                        studentsWriteRecords =new ArrayList<>();
                                    }
                                    if (pCurrentMenu.getSelectItem() < pCurrentMenu.getShowEndItem()) {
                                        Integer selectItem = pCurrentMenu.getSelectItem();
                                        if(selectItem+1<pCurrentMenu.getPItems().size()) {
                                            selectItem = selectItem + 1;
                                            pCurrentMenu.setSelectItem(selectItem);

                                            nMenuUpdate(out, writer);
                                        }
                                    } else {
                                        if (pCurrentMenu.getSelectItem() < pCurrentMenu.getMaxItems() - 1) {
                                            Integer selectItem = pCurrentMenu.getSelectItem();
                                            if(selectItem+1<pCurrentMenu.getPItems().size()) {
                                                Integer selectId = pCurrentMenu.getSelectItem() + 1;
                                                pCurrentMenu.setShowStartItem(selectId);
                                                pCurrentMenu.setShowEndItem(pCurrentMenu.getShowEndItem() + 1);
                                                pCurrentMenu.setSelectItem(selectId);
                                                nMenuUpdate(out, writer);
                                            }
                                        }
                                    }

                                }else if(emendflag){
                                    if(lastList!=null&&lastList.size()>0&&homeId!=null&&page_num!=null&&page_num>1){
                                        studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()), homeId,"2", page_num-1, lastList,false);
                                        lastList = new ArrayList<>();
                                    }
                                    String name = null;
                                    //保存作业记录
                                    if(studentsEmendRecords.size()>0&&pCurrentMenu!=null&&relation!=null){
                                        MenuItemT itemT = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem());
                                        name = itemT.getDesc();
                                        Long homeworkId = itemT.getObjectId();
                                        Integer pageN = 1;
                                        if(name.contains(" ")) {
                                            int num = name.lastIndexOf(" ");
                                            pageN = Integer.valueOf(name.substring(num+1, name.length()));
                                        }else{
                                            pageN = 1;
                                        }
                                        studentsHomeworkNewService.saveWriteRecords(Long.parseLong(relation.getUserId()),homeworkId,"2",pageN,studentsEmendRecords,false);
                                        studentsEmendRecords =new ArrayList<>();
                                    }
                                    if (pCurrentMenu.getSelectItem() < pCurrentMenu.getShowEndItem()) {
                                        Integer selectItem = pCurrentMenu.getSelectItem();
                                        if(selectItem+1<pCurrentMenu.getPItems().size()) {
                                            selectItem = selectItem + 1;
                                            pCurrentMenu.setSelectItem(selectItem);
                                            nMenuUpdate(out, writer);
                                        }

                                    } else {
                                        if (pCurrentMenu.getSelectItem() < pCurrentMenu.getMaxItems() - 1) {
                                            Integer selectItem = pCurrentMenu.getSelectItem();
                                            if(selectItem+1<pCurrentMenu.getPItems().size()) {
                                                Integer selectId = pCurrentMenu.getSelectItem() + 1;
                                                pCurrentMenu.setShowStartItem(selectId);
                                                pCurrentMenu.setShowEndItem(pCurrentMenu.getShowEndItem() + 1);
                                                pCurrentMenu.setSelectItem(selectId);
                                                nMenuUpdate(out, writer);
                                            }
                                        }
                                    }
                                }else if(feedbackflag) {
                                    //保存反馈数据
                                    if (studentsFeedbackRecords.size() > 0 && pCurrentMenu != null  && relation != null) {
                                        MenuItemT itemT = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem());
                                        String name = itemT.getDesc();
                                      //System.out.println("=============保存反馈数据====科目："+name);
                                        studentsHomeworkNewService.saveFeedbackRecords(Long.parseLong(relation.getUserId()), name,studentsFeedbackRecords);
                                      //System.out.println("=============保存反馈数据完成====");

                                    }
                                    if (pCurrentMenu.getSelectItem() < pCurrentMenu.getShowEndItem()) {
                                        Integer selectItem = pCurrentMenu.getSelectItem();
                                        if(selectItem+1<pCurrentMenu.getPItems().size()) {
                                            selectItem = selectItem + 1;
                                            pCurrentMenu.setSelectItem(selectItem);
                                            nMenuUpdate(out, writer);
                                        }
                                    } else {
                                        if (pCurrentMenu.getSelectItem() < pCurrentMenu.getMaxItems() - 1) {
                                            Integer selectItem = pCurrentMenu.getSelectItem();
                                            if(selectItem+1<pCurrentMenu.getPItems().size()) {
                                                pCurrentMenu.setShowStartItem(pCurrentMenu.getSelectItem() + 1);
                                                pCurrentMenu.setShowEndItem(pCurrentMenu.getShowEndItem() + 1);
                                                pCurrentMenu.setSelectItem(pCurrentMenu.getSelectItem() + 1);
                                                nMenuUpdate(out, writer);
                                            }
                                        }
                                    }
                                }else if(errorTitleflag) {
                                    //保存反馈数据
                                    if (uploadErrorTitleRecords.size() > 0 && pCurrentMenu != null  && relation != null) {
                                        MenuItemT itemT = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem());
                                        String name = itemT.getDesc();
                                      //System.out.println("=============保存错题上传数据====科目："+name);
                                        studentsHomeworkNewService.saveErrorTitleRecords(Long.parseLong(relation.getUserId()), name,uploadErrorTitleRecords);
                                      //System.out.println("=============保存错题上传数据完成====");

                                    }
                                    if (pCurrentMenu.getSelectItem() < pCurrentMenu.getShowEndItem()) {
                                        Integer selectItem = pCurrentMenu.getSelectItem();
                                        if(selectItem+1<pCurrentMenu.getPItems().size()) {
                                            selectItem = selectItem + 1;
                                            pCurrentMenu.setSelectItem(selectItem);
                                            nMenuUpdate(out, writer);
                                        }
                                    } else {
                                        if (pCurrentMenu.getSelectItem() < pCurrentMenu.getMaxItems() - 1) {
                                            Integer selectItem = pCurrentMenu.getSelectItem();
                                            if(selectItem+1<pCurrentMenu.getPItems().size()) {
                                                pCurrentMenu.setShowStartItem(pCurrentMenu.getSelectItem() + 1);
                                                pCurrentMenu.setShowEndItem(pCurrentMenu.getShowEndItem() + 1);
                                                pCurrentMenu.setSelectItem(pCurrentMenu.getSelectItem() + 1);
                                                nMenuUpdate(out, writer);
                                            }
                                        }
                                    }
                                }else if(pCurrentMenu!=null&&mrnuflag){
                                    if (pCurrentMenu.getSelectItem() < pCurrentMenu.getShowEndItem()) {
                                        Integer selectItem = pCurrentMenu.getSelectItem();
                                        if(selectItem+1<pCurrentMenu.getPItems().size()){
                                            selectItem = selectItem + 1;
                                            pCurrentMenu.setSelectItem(selectItem);
                                            nMenuUpdate(out, writer);
                                        }

                                    } else {
                                        if (pCurrentMenu.getSelectItem() < pCurrentMenu.getMaxItems() - 1) {
                                            Integer selectItem = pCurrentMenu.getSelectItem();
                                            if(selectItem+1<pCurrentMenu.getPItems().size()) {
                                                pCurrentMenu.setShowStartItem(pCurrentMenu.getSelectItem() + 1);
                                                pCurrentMenu.setShowEndItem(pCurrentMenu.getShowEndItem() + 1);
                                                pCurrentMenu.setSelectItem(pCurrentMenu.getSelectItem() + 1);
                                                nMenuUpdate(out, writer);
                                            }
                                        }
                                    }

                                }else{
                                    messagingTemplate.convertAndSend("/topic/nextPage/"+relation.getUserId(), relation.getUserId());
                                }
                                if(pCurrentMenu!=null) {
                                    MenuItemT itemT = pCurrentMenu.getPItems().get(pCurrentMenu.getSelectItem());
                                    if (itemT.getObjectId() != null) {
                                        String name = itemT.getDesc();
                                        homeId = itemT.getObjectId();
                                        if (name.contains(" ")) {
                                            int num = name.lastIndexOf(" ");
                                            page_num = Integer.valueOf(name.substring(num + 1, name.length()));
                                        } else {
                                            page_num = 1;
                                        }
                                    }
                                }

                              //System.out.println("++++++++++++当前页数："+page_num+"++++++++");
                            }

                            //按键松开
                            if(0==result.getButton()){
                              //System.out.println("++++++++++++按键松开++++++++++");

                            }
                        }else{
                          //System.out.println("=========没有获取到学生信息！======");
                            relation=smartDeviceUserRelationService.selectByIpAddress(clientIP);
                            if(relation==null) {
                              //System.out.println("请检测" + clientIP + "与学生的绑定或者重启智能版");
                                writer.println("请检测" + clientIP + "与学生的绑定或者重启智能版");
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
            // 关闭连接时取消心跳包定时任务
            cancelHeartbeat();
            // 确保关闭客户端连接
            try {
                clientSocket.close();
              //System.out.println("客户端断开: " + clientSocket.getInetAddress());
            } catch (IOException e) {
                System.err.println("关闭连接时出错: " + e.getMessage());
            }
        }
    }
    // 添加初始化心跳包定时发送器方法
    private void initHeartbeatScheduler(OutputStream out) {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                sendHeartbeat(out);
            } catch (IOException e) {
                System.err.println("发送心跳包失败: " + e.getMessage());
                cancelHeartbeat();
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
    }
    // 添加发送心跳包方法
    private void sendHeartbeat(OutputStream out) throws IOException {
        // 创建心跳包数据（一个字节）
        byte heartbeatData = 0x01; // 简单的心跳包数据内容

        // 创建数据包（包头+长度+类型+数据+校验和）
        byte[] packet = new byte[5]; // 包头(2)+长度(1)+类型(1)+数据(1)+校验和(1)

        // 设置包头
        packet[0] = 0x55;
        packet[1] = 0x56;

        // 设置长度字段（数据长度+1）
        packet[2] = 0x02; // 心跳包数据长度+1 = 1+1 = 2

        // 设置数据类型
        packet[3] = HEARTBEAT_TYPE;
        // 计算校验和
        int checksum = 0;
        if(isBluetooth){
            byte[] macByte=ParseTcpDataUtil.intToByteArray(mac);
            // 复制数据到缓冲区
            System.arraycopy(macByte, 0, packet, 4, 6);
            // 设置心跳数据
            packet[11] = heartbeatData;
            // 计算校验和
            checksum = calculateChecksum(packet, 0, 10); // 计算前11个字节的校验和
        }else{
            // 设置心跳数据
            packet[4] = heartbeatData;

            // 计算校验和
            checksum = calculateChecksum(packet, 0, 4); // 计算前5个字节的校验和
        }

        byte checksumByte = (byte)(checksum & 0xFF);

        // 创建完整的数据包
        byte[] fullPacket = new byte[6]; // 包头(2)+长度(1)+类型(1)+数据(1)+校验和(1)
        System.arraycopy(packet, 0, fullPacket, 0, 5);
        fullPacket[5] = checksumByte;

        // 发送心跳包
        out.write(fullPacket);
        out.flush();
      //System.out.println("发送心跳包: " + Arrays.toString(fullPacket));
    }

    // 添加计算校验和的辅助方法（从ParseTcpDataUtil类复制）
    private int calculateChecksum(byte[] data, int start, int end) {
        int sum = 0;
        for (int i = start; i <= end; i++) {
            sum += (data[i] & 0xFF); // 转为无符号整数累加
        }
        return sum;
    }

    // 添加取消心跳包定时任务的方法
    private void cancelHeartbeat() {
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            heartbeatScheduler.shutdownNow();
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
      //System.out.println("__________________+++++输出屏幕开始++++++++___________________");
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
      //System.out.println("+++++++++"+out.toString()+",长度:"+len);
        // 创建长度为len的子数组并发送
        // 对应C++的tcp_send(sl, buff, len)
        if (len > 0 && out != null) {
            byte[] sendData = new byte[len];
            System.arraycopy(buff, 0, sendData, 0, len);
          //System.out.println(new String(sendData, StandardCharsets.UTF_16LE));
            if(isBluetooth){
                ParseTcpDataUtil.sendLcdDisplayDataBluetooth(out, sendData, len,mac);
            }else {
                ParseTcpDataUtil.sendLcdDisplayData(out, sendData, len);
            }
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

      //System.out.println(signature.toString()+",签名标识：" +new String(signature));
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
