package com.jlm.homework.socket;

import lombok.Data;

@Data
public class SubjectParseResult extends ParseResult{
    private String subject;

    @Override
    public String toString() {
        return "手写板唯一标识TCP包解析结果:" +
                "1. 包头：0x" + Integer.toHexString(super.getHeader()[0] & 0xFF) + "、0x" + Integer.toHexString(super.getHeader()[1] & 0xFF) + "\n" +
                "2. 数据长度：" + super.getLength() + "字节（Type1字节 + Packet" + (super.getLength()-1) + "字节）\n" +
                "3. 数据类型：0x" + Integer.toHexString(super.getType() & 0xFF)  + "（序列号）\n" +
                "4. 科目：subject=" + subject + "\n" +
                "5. 校验和：0x" + Integer.toHexString(super.getChecksum() & 0xFF) + "（" + (super.isChecksumValid() ? "验证通过" : "验证失败") + "）";
    }
}
