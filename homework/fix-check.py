import re

path = "src/main/java/com/jlm/homework/service/impl/WrongTitleBookServiceImpl.java"
with open(path, "r", encoding="utf-8") as f:
    code = f.read()

print("Contains setDuplicateStatus(3) in '不同学生的错题':", "wrongTitleBook.setDuplicateStatus(3);" in code and "// 班级里不同学生的错题" in code)
