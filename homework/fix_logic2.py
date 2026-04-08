import re

path = "src/main/java/com/jlm/homework/service/impl/WrongTitleBookServiceImpl.java"
with open(path, "r", encoding="utf-8") as f:
    code = f.read()

old_code = """                    // 过滤掉被合并的题
                    list.add(criteriaBuilder.notEqual(root.get("duplicateStatus"), 2));
                    list.add(criteriaBuilder.notEqual(root.get("duplicateStatus"), 1));
                } catch (Exception e) {"""

new_code = """                    // 过滤掉被合并的题
                    list.add(criteriaBuilder.notEqual(root.get("duplicateStatus"), 2));
                    list.add(criteriaBuilder.notEqual(root.get("duplicateStatus"), 1));
                    
                    // 班级错题本，过滤掉别人已经错过的（同一道题班级只展示一次，也就是只展示母题）
                    // 这里的getPage如果用于班级错题本并且不指定学生ID，则过滤
                    if (wrongTitleBook.getStudentId() == null) {
                        list.add(criteriaBuilder.isNull(root.get("duplicateOf")));
                    }
                } catch (Exception e) {"""

if old_code in code:
    code = code.replace(old_code, new_code)
    with open(path, "w", encoding="utf-8") as f:
        f.write(code)
    print("Success")
else:
    print("Not found")
