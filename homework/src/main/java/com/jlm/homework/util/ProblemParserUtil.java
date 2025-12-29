package com.jlm.homework.util;

import com.jlm.homework.entity.MathProblem;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 题目解析工具类
 * 将文本格式的题目解析为List<MathProblem>
 */
public class ProblemParserUtil {

    /**
     * 解析数学题目文本为List<MathProblem>
     * @param textContent 题目文本内容
     * @return 解析后的题目列表
     */
    public static List<MathProblem> parseMathProblems(String textContent) {
        List<MathProblem> problems = new ArrayList<>();
        
        // 分割大题
        String[] bigSections = textContent.split("####\\s*[一二三四五六七八九十]+、");
        
        for (int bigNum = 1; bigNum < bigSections.length; bigNum++) {
            String bigSection = bigSections[bigNum].trim();
            String sectionType = extractSectionType(bigSection);
            
            // 使用正则表达式提取每道小题
            Pattern pattern = Pattern.compile(
                "\\s+(\\d+)\\.\\s*\\*\\*小题号\\*\\*：\\s+(\\d+)\\s*" +
                "\\*\\*题内容\\*\\*：\\s+([^\\*]+)" +
                "\\*\\*题类型\\*\\*：\\s+([^\\*]+)" +
                "\\*\\*考察知识点\\*\\*：\\s+([^\\*]+)" +
                "\\*\\*答题是否正确\\*\\*：\\s+([^\\*]+)" +
                "\\*\\*参考答案\\*\\*：\\s+([^\\*]+)" +
                "\\*\\*解析\\*\\*：\\s+([^\\*]+)",
                Pattern.DOTALL
            );
            Matcher matcher = pattern.matcher(bigSection);
            
            while (matcher.find()) {
                MathProblem problem = new MathProblem();
                problem.setBigQuestionNum(bigNum);
                problem.setSmallQuestionNum(Integer.parseInt(matcher.group(2)));
                problem.setContent(matcher.group(3).trim());
                problem.setType(matcher.group(4).trim());
                problem.setKnowledgePoint(matcher.group(5).trim());
                problem.setCorrectness(matcher.group(6).trim());
                problem.setAnswer(matcher.group(7).trim());
                problem.setAnalysis(matcher.group(8).trim());
                
                // 对于选择题，尝试提取选项
                if ("选择".equals(problem.getType())) {
                    problem.setOptions(extractOptions(problem.getContent()));
                }
                
                problems.add(problem);
            }
        }
        
        return problems;
    }
    
    /**
     * 提取大题类型（填空、判断、选择等）
     */
    private static String extractSectionType(String section) {
        String[] lines = section.split("\\n");
        if (lines.length > 0) {
            String firstLine = lines[0].trim();
            // 提取大题类型（如：填空、判断、选择）
            Pattern pattern = Pattern.compile("^(\\S+).*");
            Matcher matcher = pattern.matcher(firstLine);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "未知";
    }
    
    /**
     * 从选择题内容中提取选项
     */
    private static String extractOptions(String content) {
        // 简单的选项提取逻辑，根据具体格式可以调整
        Pattern optionPattern = Pattern.compile("([①②③④⑤⑥⑦⑧])\\s+([^①②③④⑤⑥⑦⑧]+)");
        Matcher optionMatcher = optionPattern.matcher(content);
        StringBuilder options = new StringBuilder();
        
        while (optionMatcher.find()) {
            if (options.length() > 0) {
                options.append("; ");
            }
            options.append(optionMatcher.group(1)).append(":").append(optionMatcher.group(2).trim());
        }
        
        return options.toString();
    }
    
    /**
     * 测试解析方法
     */
    public static void main(String[] args) {
        // 示例使用
        String textContent = "#### 一、填空。（第6题4分，其余每空1分，共19分）  \\n 1. **小题号**：1  \\n    **题内容**：一个数由2个十、2个十分之一、3个百分之一和6个千分之一组成，这个数是( )，读作( )。  \\n    **题类型**：填空  \\n    **考察知识点**：小数的组成与读写  \\n    **答题是否正确**：暂无  \\n    **参考答案**：(20.236，二十点二三六)  \\n    **解析**：2个十是20，2个十分之一是0.2，3个百分之一是0.03，6个千分之一是0.006。";
        
        List<MathProblem> problems = parseMathProblems(textContent);
        //System.out.println("解析出" + problems.size() + "道题目");
    }
}