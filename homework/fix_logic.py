import re

path = "src/main/java/com/jlm/homework/service/impl/WrongTitleBookServiceImpl.java"
with open(path, "r", encoding="utf-8") as f:
    code = f.read()

old_code = """                                if (maxSimilarity >= 0.8 && duplicateOf != null) {
                                    // 确定是重复题：自动标记状态为2 (已合并/废弃)
                                    wrongTitleBook.setDuplicateStatus(2);
                                    wrongTitleBook.setDuplicateOf(duplicateOf);

                                    // 更新母题的错误次数
                                    Optional<WrongTitleBook> parentOpt = wrongTitleBookRepository.findById(duplicateOf);
                                    if (parentOpt.isPresent()) {
                                        WrongTitleBook parentBook = parentOpt.get();
                                        Integer oldErrorCount = parentBook.getErrorCount() == null ? 1 : parentBook.getErrorCount();
                                        parentBook.setErrorCount(oldErrorCount + 1);
                                        wrongTitleBookRepository.save(parentBook);
                                    }
                                } else {"""

new_code = """                                if (maxSimilarity >= 0.8 && duplicateOf != null) {
                                    wrongTitleBook.setDuplicateOf(duplicateOf);

                                    // 更新母题的错误次数
                                    Optional<WrongTitleBook> parentOpt = wrongTitleBookRepository.findById(duplicateOf);
                                    if (parentOpt.isPresent()) {
                                        WrongTitleBook parentBook = parentOpt.get();
                                        if (parentBook.getStudentId() != null && wrongTitleBook.getStudentId() != null && parentBook.getStudentId().equals(wrongTitleBook.getStudentId())) {
                                            // 同一个学生的相同错题，废弃新题，次数+1
                                            wrongTitleBook.setDuplicateStatus(2);
                                            Integer oldErrorCount = parentBook.getErrorCount() == null ? 1 : parentBook.getErrorCount();
                                            parentBook.setErrorCount(oldErrorCount + 1);
                                            wrongTitleBookRepository.save(parentBook);
                                        } else {
                                            // 班级里不同学生的错题
                                            Integer oldErrorCount = parentBook.getErrorCount() == null ? 1 : parentBook.getErrorCount();
                                            parentBook.setErrorCount(oldErrorCount + 1);
                                            wrongTitleBookRepository.save(parentBook);
                                            
                                            // 班级本去重，不展示这条（或者用其他方法去重）
                                            // 为了让该学生在个人错题本能看到这题，状态应为3
                                            // 可以在查询班级错题时排除 duplicateOf != null 的记录
                                            wrongTitleBook.setDuplicateStatus(3);
                                        }
                                    }
                                } else {"""

if old_code in code:
    code = code.replace(old_code, new_code)
    with open(path, "w", encoding="utf-8") as f:
        f.write(code)
    print("Success")
else:
    print("Not found")
