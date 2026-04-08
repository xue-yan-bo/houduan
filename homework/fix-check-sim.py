import re

path = "src/main/java/com/jlm/homework/service/impl/WrongTitleBookServiceImpl.java"
with open(path, "r", encoding="utf-8") as f:
    code = f.read()
    
# look at the sim api call logic
lines = code.split("\n")
for i, line in enumerate(lines):
    if "Result<SimilarityResultDto> simResult =" in line:
        for j in range(i-5, i+20):
            print(lines[j])
        break
