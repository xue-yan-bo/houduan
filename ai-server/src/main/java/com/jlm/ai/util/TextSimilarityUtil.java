package com.jlm.ai.util;

import com.alibaba.cloud.commons.lang.StringUtils;

public class TextSimilarityUtil {

    public static double getSimilarity(String s1, String s2) {
        if (s1 == null || s1.isEmpty()) {
            if (s2 == null || s2.isEmpty()) return 1.0;
            return 0.0;
        }
        if (s2 == null || s2.isEmpty()) return 0.0;

        // 去除所有的空白字符和中文标点符号（通过Unicode块匹配），避免因为标点差异影响相似度
        String str1 = s1.replaceAll("[\\s\\p{Punct}\\u3000-\\u303F\\uFF00-\\uFFEF]+", "");
        String str2 = s2.replaceAll("[\\s\\p{Punct}\\u3000-\\u303F\\uFF00-\\uFFEF]+", "");

        if (str1.length() == 0 && str2.length() == 0) return 1.0;
        if (str1.length() == 0 || str2.length() == 0) return 0.0;

        int maxLength = Math.max(str1.length(), str2.length());
        int distance = levenshteinDistance(str1, str2);

        return 1.0 - ((double) distance / maxLength);
    }

    private static int levenshteinDistance(CharSequence lhs, CharSequence rhs) {
        int[][] distance = new int[lhs.length() + 1][rhs.length() + 1];

        for (int i = 0; i <= lhs.length(); i++) distance[i][0] = i;
        for (int j = 1; j <= rhs.length(); j++) distance[0][j] = j;

        for (int i = 1; i <= lhs.length(); i++) {
            for (int j = 1; j <= rhs.length(); j++) {
                int cost = (lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1;
                distance[i][j] = Math.min(
                        Math.min(distance[i - 1][j] + 1, distance[i][j - 1] + 1),
                        distance[i - 1][j - 1] + cost
                );
            }
        }
        return distance[lhs.length()][rhs.length()];
    }
}
