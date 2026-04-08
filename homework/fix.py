import re
file_path = r"src\main\java\com\jlm\homework\service\impl\WrongTitleBookServiceImpl.java"
with open(file_path, "r", encoding="utf-8") as f:
    text = f.read()

# Replace the messy line
text = re.sub(
    r"try \{ Thread\.sleep\(\(long\)\(Math\.random\(\) \* 2000\)\); \} catch \(InterruptedException e\) \{ e\.printStackTrace\(\); \} if \(!historyList\.isEmpty\(\)\) \{ Result<SimilarityResultDto> simResult = aiFeignClient\.checkSimilarity\(requestDto\);",
    "try { Thread.sleep((long)(Math.random() * 2000)); } catch (InterruptedException e) { e.printStackTrace(); }\n                                if (!historyList.isEmpty()) {\n                                    Result<SimilarityResultDto> simResult = aiFeignClient.checkSimilarity(requestDto);",
    text
)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(text)
