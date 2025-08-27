package com.jlm.homework.socket;

import lombok.Data;
/**
 * 手写数据解析结果封装类
 */
@Data
public class HandwritingParseResult extends ParseResult {
    private int x;              // X坐标
    private int y;              // Y坐标
    private int pressure;       // 压力值
    private long timestamp;     // 时间戳（ms，手写板开机起始）

    @Override
    public String toString() {
        return "手写数据TCP包解析结果：\n" +
                "1. 包头：0x" + Integer.toHexString(super.getHeader()[0] & 0xFF) + "、0x" + Integer.toHexString(super.getHeader()[1] & 0xFF) + "\n" +
                "2. 数据长度：" + super.getLength() + "字节（Type1字节 + Packet" + (super.getLength()-1) + "字节）\n" +
                "3. 数据类型：0x" + Integer.toHexString(super.getType() & 0xFF) + "（手写数据）\n" +
                "4. 手写坐标：X=" + x + "，Y=" + y + "\n" +
                "5. 手写压力值：" + pressure + "\n" +
                "6. 时间戳：" + timestamp + "ms（手写板开机后）\n" +
                "7. 校验和：0x" + Integer.toHexString(super.getChecksum() & 0xFF) + "（" + (super.isChecksumValid() ? "验证通过" : "验证失败") + "）";
    }
}
