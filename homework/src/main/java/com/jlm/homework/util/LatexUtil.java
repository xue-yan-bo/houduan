package com.jlm.homework.util;

import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author QingYang
 * @version 1.0
 * @description
 * @date 2023/3/25 0025
 */
public class LatexUtil {


    /**
     * 这里spire.doc有一些缺陷，对于一些符号支持的不是很好，大于等于小于等于，这里做一下替换，连续中文也做了\mbox{}包裹，这个是基于latex的经验，但是实际并没有什么用，spire.doc不支持带有中文的latex公式渲染，可能版本太低了，所以不能正常渲染就直接显示图片
     */
    public static String latexFormat(String latex) {
        if (latex.contains("leqslant")) {
            latex = latex.replace("leqslant", "leq");
        }
        if (latex.contains("geqslant")) {
            latex = latex.replace("geqslant", "geq");
        }
        StringBuilder latexBuilder = new StringBuilder();
        boolean isChinese = false;
        String regexStr = "[\u4E00-\u9FA5]";
        for (Character c : latex.toCharArray()) {
            Matcher chineseMatch = Pattern.compile(regexStr).matcher(c.toString());
            if (chineseMatch.find()) {
                if (isChinese) {
                    latexBuilder.append(c);
                } else {
                    latexBuilder.append("\\mbox{").append(c);
                    isChinese = true;
                }
                continue;
            } else {
                if (isChinese) {
                    isChinese = false;
                    latexBuilder.append("}");
                }
                latexBuilder.append(c);
            }
        }
        return latexBuilder.toString();
    }

    public static Image latex2Image(String latexStr, float fontSize) {
        int styleDisplay = TeXConstants.STYLE_DISPLAY;
        Color fontColor = Color.BLACK;
        Color bg = null;
        return TeXFormula.createBufferedImage(latexStr, styleDisplay, fontSize, fontColor, bg);
    }
}
