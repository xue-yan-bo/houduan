package com.jlm.homework;

import com.jlm.homework.util.AIUtil;
import com.jlm.homework.util.QianWenAIUtil;
import com.jlm.homework.util.TongYiServ;

public class TestAiF {
    public static void main(String[] args) throws Exception {
        QianWenAIUtil util = new QianWenAIUtil();
        util.setAiName("qianwen");
        TongYiServ tongYiServ = new TongYiServ();
        util.setTongYiServ(tongYiServ);
        String res = util.analyzeText("你好");
        System.out.println("Result: " + res);
    }
}
