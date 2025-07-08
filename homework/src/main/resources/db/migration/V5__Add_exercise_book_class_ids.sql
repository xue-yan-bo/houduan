-- 为练习册表添加班级ID列表字段
-- 支持存储多个班级ID（JSON格式）

-- 添加班级ID列表字段
ALTER TABLE exercise_book
ADD COLUMN class_ids JSON COMMENT '班级ID列表（JSON格式）' AFTER class_id;

-- 添加班级名称列表字段
ALTER TABLE exercise_book
ADD COLUMN class_names JSON COMMENT '班级名称列表（JSON格式）' AFTER class_ids;

-- 迁移现有数据：将现有的class_id数据迁移到class_ids字段中
UPDATE exercise_book 
SET class_ids = JSON_ARRAY(class_id) 
WHERE class_id IS NOT NULL;

-- 添加索引以提高查询性能（MySQL 5.7+支持JSON索引）
-- 注意：如果MySQL版本较低，可能需要注释掉这行
-- CREATE INDEX idx_exercise_book_class_ids ON exercise_book((CAST(class_ids AS UNSIGNED ARRAY)));

-- 示例数据更新（可选）
-- 为现有的练习册添加多个班级ID示例
-- UPDATE exercise_book 
-- SET class_ids = JSON_ARRAY(1, 2, 3) 
-- WHERE id = 1;

-- UPDATE exercise_book 
-- SET class_ids = JSON_ARRAY(2, 4) 
-- WHERE id = 2;

-- 添加约束检查（可选）
-- 确保class_ids字段如果不为空，则必须是有效的JSON数组
-- ALTER TABLE exercise_book 
-- ADD CONSTRAINT chk_exercise_book_class_ids 
-- CHECK (class_ids IS NULL OR JSON_VALID(class_ids));

-- 注释说明：
-- 1. class_ids字段使用JSON类型存储班级ID数组
-- 2. 格式示例：[1, 2, 3] 表示关联班级ID为1、2、3
-- 3. 保留原有的class_id字段以保持向后兼容
-- 4. 查询时可以使用JSON函数进行条件过滤
