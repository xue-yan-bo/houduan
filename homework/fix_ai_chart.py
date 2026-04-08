import sys
import re

file_path = r"src\main\java\com\jlm\homework\service\impl\WrongTitleBookServiceImpl.java"

with open(file_path, "r", encoding="utf-8") as f:
    text = f.read()

# Make the changes cleanly
search_block = """                            if (wrongTitleBook.getClassId() != null) {
                                WrongTitleBook search = new WrongTitleBook();
                                search.setClassId(wrongTitleBook.getClassId());"""

replace_block = """                            if (wrongTitleBook.getClassId() != null) {
                                try { Thread.sleep((long)(Math.random() * 2000)); } catch (InterruptedException e) { e.printStackTrace(); }
                                synchronized (WrongTitleBookServiceImpl.class) {
                                WrongTitleBook search = new WrongTitleBook();
                                search.setClassId(wrongTitleBook.getClassId());"""

text = text.replace(search_block, replace_block)

search_block2 = """                                if (maxSimilarity >= 0.8 && duplicateOf != null) {
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
                                } else {
                                    // 新题，或者相似度小于0.8
                                    wrongTitleBook.setDuplicateStatus(3);
                                }
                            } else {"""

replace_block2 = """                                if (maxSimilarity >= 0.8 && duplicateOf != null) {
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
                                } else {
                                    // 新题，或者相似度小于0.8
                                    wrongTitleBook.setDuplicateStatus(3);
                                }
                                }
                            } else {"""

text = text.replace(search_block2, replace_block2)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(text)

