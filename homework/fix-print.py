import re

path = "src/main/java/com/jlm/homework/service/impl/WrongTitleBookServiceImpl.java"
with open(path, "r", encoding="utf-8") as f:
    code = f.read()

old_code = """                                Result<SimilarityResultDto> simResult = aiFeignClient.checkSimilarity(requestDto);
                                if(simResult != null && simResult.getCode() == 200 && simResult.getData() != null) {"""

new_code = """                                Result<SimilarityResultDto> simResult = aiFeignClient.checkSimilarity(requestDto);
                                System.out.println("====== simResult: " + com.alibaba.fastjson.JSON.toJSONString(simResult));
                                if(simResult != null && simResult.getCode() == 200 && simResult.getData() != null) {"""

if old_code in code:
    code = code.replace(old_code, new_code)
    with open(path, "w", encoding="utf-8") as f:
        f.write(code)
    print("Success")
else:
    print("Not found")
