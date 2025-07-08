-- 为练习册表添加ID字段
-- 添加学科ID、年级ID、班级ID字段

-- 添加学科ID字段
ALTER TABLE exercise_book 
ADD COLUMN subject_id BIGINT COMMENT '学科ID' AFTER subject;

-- 添加年级ID字段
ALTER TABLE exercise_book 
ADD COLUMN grade_id BIGINT COMMENT '年级ID' AFTER grade;

-- 添加班级ID字段
ALTER TABLE exercise_book 
ADD COLUMN class_id BIGINT COMMENT '班级ID' AFTER grade_id;

-- 添加索引以提高查询性能
CREATE INDEX idx_exercise_book_subject_id ON exercise_book(subject_id);
CREATE INDEX idx_exercise_book_grade_id ON exercise_book(grade_id);
CREATE INDEX idx_exercise_book_class_id ON exercise_book(class_id);

-- 更新现有数据的示例（可根据实际情况调整）
-- 这里假设有一些基础的学科、年级、班级数据
-- 实际项目中需要根据具体的业务数据进行更新

-- 示例：为现有的练习册设置一些默认的ID值
-- UPDATE exercise_book SET subject_id = 1 WHERE subject = '数学';
-- UPDATE exercise_book SET subject_id = 2 WHERE subject = '语文';
-- UPDATE exercise_book SET subject_id = 3 WHERE subject = '英语';
-- UPDATE exercise_book SET subject_id = 4 WHERE subject = '科学';

-- UPDATE exercise_book SET grade_id = 3 WHERE grade = '三年级';
-- UPDATE exercise_book SET grade_id = 4 WHERE grade = '四年级';
-- UPDATE exercise_book SET grade_id = 5 WHERE grade = '五年级';
-- UPDATE exercise_book SET grade_id = 6 WHERE grade = '六年级';

-- 注释：实际使用时需要根据系统中的学科、年级、班级表的真实ID进行更新
