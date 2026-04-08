import re
import os

path = "src/main/java/com/jlm/homework/service/impl/WrongTitleBookServiceImpl.java"
with open(path, "r", encoding="utf-8") as f:
    code = f.read()

regex = r"    @Async\s*public void aiChart\(Long wrongTitleId\)\s*\{[\s\S]*?// 继续生成AI图表分析"

newAiChart = """    @Async
    public void aiChart(Long wrongTitleId) {
        Optional<WrongTitleBook> optional=wrongTitleBookRepository.findById(wrongTitleId);
        if(optional==null||!optional.isPresent()){
            return;
        }
        WrongTitleBook wrongTitleBook = optional.get();
        
        // 确保有 text 可供查重
        String text = wrongTitleBook.getTitleContext();
        if (com.alibaba.cloud.commons.lang.StringUtils.isEmpty(text)) {
            String imageUrl = null;
            if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(wrongTitleBook.getTitleImage())) {
                imageUrl = wrongTitleBook.getTitleImage();
            } else if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(wrongTitleBook.getSourceImageUrl())) {
                imageUrl = wrongTitleBook.getSourceImageUrl();
            }
            if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(imageUrl)) {
                try {
                    Result<String> aiResult = aiFeignClient.analyzeImage(imageUrl);
                    if (aiResult != null && aiResult.getCode() == 200) {
                        text = aiResult.getData();
                        if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(text)) {
                            wrongTitleBook.setTitleContext(text);
                            wrongTitleBookRepository.save(wrongTitleBook);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("AI提取题目文字失败: " + e.getMessage());
                }
            }
        }

        // 如果最终获取到了文本内容，则在当前学生范围内进行本地去重查重逻辑
        if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(text) && wrongTitleBook.getStudentId() != null) {
            try {
                WrongTitleBook search = new WrongTitleBook();
                search.setStudentId(wrongTitleBook.getStudentId());
                
                // 查找该学生所有的历史错题
                java.util.List<WrongTitleBook> historyBooks = wrongTitleBookRepository.findAll(org.springframework.data.domain.Example.of(search));
                
                double maxSimilarity = 0.0;
                Long duplicateOf = null;
                
                for (WrongTitleBook history : historyBooks) {
                    // 排除自己，排除没有文本内容的记录
                    if (history.getId().equals(wrongTitleBook.getId()) || com.alibaba.cloud.commons.lang.StringUtils.isEmpty(history.getTitleContext())) {
                        continue;
                    }
                    
                    // 使用现成的 TextSimilarityUtil 进行本地比对
                    double sim = com.jlm.homework.util.TextSimilarityUtil.getSimilarity(text, history.getTitleContext());
                    if (sim > maxSimilarity) {
                        maxSimilarity = sim;
                        duplicateOf = history.getId();
                    }
                }
                
                if (maxSimilarity >= 0.8 && duplicateOf != null) {
                    // 发现重复题：
                    // 1. 更新母题的错误次数加1
                    Optional<WrongTitleBook> parentOpt = wrongTitleBookRepository.findById(duplicateOf);
                    if (parentOpt.isPresent()) {
                        WrongTitleBook parentBook = parentOpt.get();
                        Integer oldErrorCount = parentBook.getErrorCount() == null ? 1 : parentBook.getErrorCount();
                        parentBook.setErrorCount(oldErrorCount + 1);
                        wrongTitleBookRepository.save(parentBook);
                    }
                    
                    // 2. 调用当前类的 deleteById 将这道新增的错题彻底删除（不入库/前端不展示）
                    this.deleteById(wrongTitleBook.getId());
                    
                    // 3. 提前结束，不再进行后续分析
                    return;
                } else {
                    // 新题，或者相似度小于0.8，标记为解析完成
                    wrongTitleBook.setDuplicateStatus(3);
                    wrongTitleBookRepository.save(wrongTitleBook);
                }
            } catch (Exception e) {
                 System.err.println("去重比对失败: " + e.getMessage());
            }
        }

        // 继续生成AI图表分析"""

if re.search(regex, code):
    code = re.sub(regex, newAiChart, code, count=1)
    with open(path, "w", encoding="utf-8") as f:
        f.write(code)
    print("Modified successfully.")
else:
    print("Pattern not found.")

