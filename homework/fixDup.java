import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class fixDup {
    public static void main(String[] args) throws Exception {
        String path = "src/main/java/com/jlm/homework/service/impl/WrongTitleBookServiceImpl.java";
        String code = new String(Files.readAllBytes(Paths.get(path)));

        String regex = "    @Async\s*public void aiChart\(Long wrongTitleId\)\s*\{[\s\S]*?// 继续生成AI图表分析";
        
        String newAiChart = "    @Async\n" +
                "    public void aiChart(Long wrongTitleId) {\n" +
                "        Optional<WrongTitleBook> optional=wrongTitleBookRepository.findById(wrongTitleId);\n" +
                "        if(optional==null||!optional.isPresent()){\n" +
                "            return;\n" +
                "        }\n" +
                "        WrongTitleBook wrongTitleBook = optional.get();\n" +
                "        \n" +
                "        // 确保有 text 可供查重\n" +
                "        String text = wrongTitleBook.getTitleContext();\n" +
                "        if (com.alibaba.cloud.commons.lang.StringUtils.isEmpty(text)) {\n" +
                "            String imageUrl = null;\n" +
                "            if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(wrongTitleBook.getTitleImage())) {\n" +
                "                imageUrl = wrongTitleBook.getTitleImage();\n" +
                "            } else if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(wrongTitleBook.getSourceImageUrl())) {\n" +
                "                imageUrl = wrongTitleBook.getSourceImageUrl();\n" +
                "            }\n" +
                "            if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(imageUrl)) {\n" +
                "                try {\n" +
                "                    Result<String> aiResult = aiFeignClient.analyzeImage(imageUrl);\n" +
                "                    if (aiResult != null && aiResult.getCode() == 200) {\n" +
                "                        text = aiResult.getData();\n" +
                "                        if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(text)) {\n" +
                "                            wrongTitleBook.setTitleContext(text);\n" +
                "                            wrongTitleBookRepository.save(wrongTitleBook);\n" +
                "                        }\n" +
                "                    }\n" +
                "                } catch (Exception e) {\n" +
                "                    System.err.println(\"AI提取题目文字失败: \" + e.getMessage());\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        // 如果最终获取到了文本内容，则在当前学生范围内进行本地去重查重逻辑\n" +
                "        if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(text) && wrongTitleBook.getStudentId() != null) {\n" +
                "            try {\n" +
                "                WrongTitleBook search = new WrongTitleBook();\n" +
                "                search.setStudentId(wrongTitleBook.getStudentId());\n" +
                "                \n" +
                "                // 查找该学生所有的历史错题\n" +
                "                java.util.List<WrongTitleBook> historyBooks = wrongTitleBookRepository.findAll(org.springframework.data.domain.Example.of(search));\n" +
                "                \n" +
                "                double maxSimilarity = 0.0;\n" +
                "                Long duplicateOf = null;\n" +
                "                \n" +
                "                for (WrongTitleBook history : historyBooks) {\n" +
                "                    // 排除自己，排除没有文本内容的记录\n" +
                "                    if (history.getId().equals(wrongTitleBook.getId()) || com.alibaba.cloud.commons.lang.StringUtils.isEmpty(history.getTitleContext())) {\n" +
                "                        continue;\n" +
                "                    }\n" +
                "                    \n" +
                "                    // 使用现成的 TextSimilarityUtil 进行本地比对\n" +
                "                    double sim = com.jlm.homework.util.TextSimilarityUtil.getSimilarity(text, history.getTitleContext());\n" +
                "                    if (sim > maxSimilarity) {\n" +
                "                        maxSimilarity = sim;\n" +
                "                        duplicateOf = history.getId();\n" +
                "                    }\n" +
                "                }\n" +
                "                \n" +
                "                if (maxSimilarity >= 0.8 && duplicateOf != null) {\n" +
                "                    // 发现重复题：\n" +
                "                    // 1. 更新母题的错误次数加1\n" +
                "                    Optional<WrongTitleBook> parentOpt = wrongTitleBookRepository.findById(duplicateOf);\n" +
                "                    if (parentOpt.isPresent()) {\n" +
                "                        WrongTitleBook parentBook = parentOpt.get();\n" +
                "                        Integer oldErrorCount = parentBook.getErrorCount() == null ? 1 : parentBook.getErrorCount();\n" +
                "                        parentBook.setErrorCount(oldErrorCount + 1);\n" +
                "                        wrongTitleBookRepository.save(parentBook);\n" +
                "                    }\n" +
                "                    \n" +
                "                    // 2. 调用当前类的 deleteById 将这道新增的错题彻底删除（不入库/前端不展示）\n" +
                "                    this.deleteById(wrongTitleBook.getId());\n" +
                "                    \n" +
                "                    // 3. 提前结束，不再进行后续分析\n" +
                "                    return;\n" +
                "                } else {\n" +
                "                    // 新题，或者相似度小于0.8，标记为解析完成\n" +
                "                    wrongTitleBook.setDuplicateStatus(3);\n" +
                "                    wrongTitleBookRepository.save(wrongTitleBook);\n" +
                "                }\n" +
                "            } catch (Exception e) {\n" +
                "                 System.err.println(\"去重比对失败: \" + e.getMessage());\n" +
                "            }\n" +
                "        }\n\n        // 继续生成AI图表分析";
        
        Matcher m = Pattern.compile(regex).matcher(code);
        if (m.find()) {
            code = m.replaceFirst(newAiChart);
            Files.write(Paths.get(path), code.getBytes());
            System.out.println("Modified successfully.");
        } else {
            System.out.println("Pattern not found.");
        }
    }
}
