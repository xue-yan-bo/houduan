import re

path = "src/main/java/com/jlm/homework/service/impl/WrongTitleBookServiceImpl.java"
with open(path, "r", encoding="utf-8") as f:
    code = f.read()

old_code = """                            // 去重逻辑结束，新题标记为3 (解析完成)，并且不能直接在 catch 中拦截阻止 save
                            wrongTitleBookRepository.save(wrongTitleBook);
                        }
                    }"""

new_code = """                            // 去重逻辑结束
                            // 注意：这里的 wrongTitleBook 可能在上面被设置为2（同一个人废弃）或3（不同人保留），直接 save
                            wrongTitleBookRepository.save(wrongTitleBook);
                        }
                    }"""

if old_code in code:
    code = code.replace(old_code, new_code)
    with open(path, "w", encoding="utf-8") as f:
        f.write(code)
    print("Success")
else:
    print("Not found")
