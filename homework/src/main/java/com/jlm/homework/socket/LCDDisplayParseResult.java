package com.jlm.homework.socket;

import lombok.Data;

/**
 * LCD显示字符串解析结果封装类
 */
@Data
public class LCDDisplayParseResult extends ParseResult {
    private String displayString; // 屏幕显示的字符串

    @Override
    public String toString() {
        return "LCD显示字符串TCP包解析结果：\n" +
                "1. 包头：0x" + Integer.toHexString(super.getHeader()[0] & 0xFF) + "、0x" + Integer.toHexString(super.getHeader()[1] & 0xFF) + "\n" +
                "2. 数据长度：" + super.getLength() + "字节（Type1字节 + Packet" + (super.getLength()-1) + "字节）\n" +
                "3. 数据类型：0x" + Integer.toHexString(super.getType() & 0xFF) + "（LCD显示字符串）\n" +
                "4. 显示字符串：" + displayString + "\n" +
                "5. 校验和：0x" + Integer.toHexString(super.getChecksum() & 0xFF) + "（" + (super.isChecksumValid() ? "验证通过" : "验证失败") + "）";
    }
}