package com.jlm.homework;

public class DuplicateLogicTest {
    
    // 直接把算法写在这里测试
    public static double getSimilarity(String s1, String s2) {
        if (s1 == null || s1.isEmpty()) {
            if (s2 == null || s2.isEmpty()) return 1.0;
            return 0.0;
        }
        if (s2 == null || s2.isEmpty()) return 0.0;

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

    public static void main(String[] args) {
        // 根据你给的截图里的数据，这两道题原本是同一道题，只是AI解析出了一些细微差距
        String historyText = "1.已知函数f(x)的定义域为(-1,0)，则f(2x+1)的定义域为( )A.(-1,1) B.C.(-1,0)";
        String newText1 = "1.已知函数f(x)的定义域为(-1,0)，则f(2x+1)的定义域为( )A.(-1,1) B. C.(-1,0) D.";
        
        // 模拟一道别的题
        String newText2 = "2.在行驶的火车上，放在桌面上的水杯，认为水杯处于静止状态所选择的参考系是";
        
        // 计算相似度
        double sim1 = getSimilarity(newText1, historyText);
        double sim2 = getSimilarity(newText2, historyText);
        
        System.out.println("相似度 (同题不同OCR识别结果，例如多出一个选项D.): " + sim1);
        System.out.println("相似度 (完全不同的两道题): " + sim2);
        System.out.println("------------------------------------");
        System.out.println("同题判断是否符合业务预期 (>0.8): " + (sim1 >= 0.8 ? "通过 (将执行合并去重和错误数+1)" : "失败"));
        System.out.println("不同题判断是否符合业务预期 (<0.8): " + (sim2 < 0.8 ? "通过 (将作为新错题录入)" : "失败"));
    }
}
