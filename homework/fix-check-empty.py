import re

path = "src/main/java/com/jlm/homework/service/impl/WrongTitleBookServiceImpl.java"
with open(path, "r", encoding="utf-8") as f:
    code = f.read()

old_code = """        if (com.alibaba.cloud.commons.lang.StringUtils.isEmpty(wrongTitleBook.getTitleContext())) {
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
                        String text = aiResult.getData();
                        if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(text)) {
                            wrongTitleBook.setTitleContext(text);
                            // ----------- 新增：异步去重查重核心逻辑 -----------"""

new_code = """        String text = wrongTitleBook.getTitleContext();
        try {
            if (com.alibaba.cloud.commons.lang.StringUtils.isEmpty(text)) {
                String imageUrl = null;
                if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(wrongTitleBook.getTitleImage())) {
                    imageUrl = wrongTitleBook.getTitleImage();
                } else if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(wrongTitleBook.getSourceImageUrl())) {
                    imageUrl = wrongTitleBook.getSourceImageUrl();
                }
                if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(imageUrl)) {
                    Result<String> aiResult = aiFeignClient.analyzeImage(imageUrl);
                    if (aiResult != null && aiResult.getCode() == 200) {
                        text = aiResult.getData();
                        if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(text)) {
                            wrongTitleBook.setTitleContext(text);
                        }
                    }
                }
            }
            
            if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(text)) {
                // ----------- 新增：异步去重查重核心逻辑 -----------"""

if old_code in code:
    code = code.replace(old_code, new_code)
    with open(path, "w", encoding="utf-8") as f:
        f.write(code)
    print("Success")
else:
    print("Not found")
