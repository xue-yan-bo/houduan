package com.jlm.homework.socket.context;

import com.jlm.homework.dto.Copybook2Board;
import com.jlm.homework.dto.HomeWork2Board;
import com.jlm.homework.entity.SmartDeviceUserRelation;
import com.jlm.homework.entity.StudentsWriteRecord;
import com.jlm.homework.socket.HandwritingParseResult;
import com.jlm.homework.socket.boardmenu.MenuT;
import lombok.Data;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * 会话上下文，保存每个客户端连接的状态信息
 */
@Data
public class SessionContext implements Serializable {
    private static final long serialVersionUID = 1L;
    // 常量
    public static final int SAVE_SIZE = 2000;

    // 用户信息
    private SmartDeviceUserRelation relation;
    private InetSocketAddress remoteAddress;
    private String clientIP;
    private int clientPort;
    private boolean isBluetooth = false;
    private String mac = null;

    // 业务状态
    private Long homeId = null;
    private Long copybookId = null;
    private Integer pageNum = null;
    private Long feedbackId = null;
    private Long errorTitleId = null;
    private String feedbackSubject = null;
    private String errorTitleSubject = null;
    private String errorTitleMode = null; // 纠错模式类型：upload(错题上传)、correction(改错模式)
    private MenuT currentMenu = null; // 当前菜单
    private Long buttonTimes = null;
    private int confirmCount = 0; // querenJishu
    private long lastHeartbeatTime = 0; // 最后心跳时间

    // 模式标志
    private boolean menuFlag = false; // mrnuflag
    private boolean homeworkFlag = false;
    private boolean emendFlag = false;
    private boolean feedbackFlag = false;
    private boolean errorTitleFlag = false;
    private boolean copybookFlag = false;
    // 菜单缓存
    private MenuT mainMenu = null;
    private MenuT homeworkMenu = null;
    private MenuT emendMenu = null;
    private MenuT feedbackMenu = null;
    private MenuT errorTitleMenu = null;
    private MenuT copybookMenu = null;
    // 数据缓存
    private List<HomeWork2Board> work2Boards = new ArrayList<>();
    private List<HomeWork2Board> emendBoards = new ArrayList<>();
    private List<Copybook2Board> copybookBoards = new ArrayList<>();
    // 笔迹数据缓存
    private List<HandwritingParseResult> studentClassRecords = java.util.Collections.synchronizedList(new ArrayList<>());
    private List<StudentsWriteRecord> studentsWriteRecords = java.util.Collections.synchronizedList(new ArrayList<>());
    private List<StudentsWriteRecord> studentsEmendRecords = java.util.Collections.synchronizedList(new ArrayList<>());
    private List<StudentsWriteRecord> studentsFeedbackRecords = java.util.Collections.synchronizedList(new ArrayList<>());
    private List<StudentsWriteRecord> uploadErrorTitleRecords = java.util.Collections.synchronizedList(new ArrayList<>());
    private List<StudentsWriteRecord> studentsCopybookRecords = java.util.Collections.synchronizedList(new ArrayList<>());
    private List<StudentsWriteRecord> lastList = java.util.Collections.synchronizedList(new ArrayList<>());

    public synchronized void clearSession() {
        this.work2Boards.clear();
        this.studentsWriteRecords.clear();
        this.studentsEmendRecords.clear();
        this.studentsFeedbackRecords.clear();
        this.uploadErrorTitleRecords.clear();
        this.studentsCopybookRecords.clear();
        this.lastList.clear();
        this.studentClassRecords.clear();
        this.homeId = null;
        this.copybookId = null;
        this.feedbackId = null;
        this.errorTitleId = null;
        this.errorTitleMode = null;
        this.pageNum = null;
        this.currentMenu = null;
        this.confirmCount = 0;
        this.homeworkFlag = false;
        this.emendFlag = false;
        this.feedbackFlag = false;
        this.errorTitleFlag = false;
        this.copybookFlag = false;
    }
}
