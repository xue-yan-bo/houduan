-- 为练习册表添加学校ID字段（租户隔离）
-- 支持多租户数据隔离

-- 添加学校ID字段
ALTER TABLE exercise_book 
ADD COLUMN school_id BIGINT COMMENT '学校ID（租户标识）' AFTER creator_id;

-- 添加索引以提高查询性能（这是租户隔离的核心索引）
CREATE INDEX idx_exercise_book_school_id ON exercise_book(school_id);

-- 创建组合索引，提高多条件查询性能
CREATE INDEX idx_exercise_book_school_creator ON exercise_book(school_id, creator_id);
CREATE INDEX idx_exercise_book_school_status ON exercise_book(school_id, status);
CREATE INDEX idx_exercise_book_school_subject ON exercise_book(school_id, subject_id);

-- 注释说明：
-- 1. school_id字段用于多租户数据隔离
-- 2. 每个练习册都必须属于一个学校
-- 3. 查询时必须带上school_id条件，确保数据隔离
-- 4. 索引设计考虑了常用的查询场景
-- 5. 创建时自动设置为当前登录用户所属学校ID
-- 6. 修改时schoolId保持不变 