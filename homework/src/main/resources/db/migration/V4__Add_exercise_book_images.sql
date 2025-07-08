-- 为练习册表添加图片字段
-- 支持存储多个图片信息（JSON格式）

-- 添加图片字段
ALTER TABLE exercise_book 
ADD COLUMN images JSON COMMENT '练习册图片列表（JSON格式）' AFTER status;

-- 添加索引以提高查询性能（MySQL 5.7+支持JSON索引）
-- 注意：如果MySQL版本较低，可能需要注释掉这行
-- CREATE INDEX idx_exercise_book_images ON exercise_book((CAST(images->'$[*].type' AS CHAR(50) ARRAY)));

-- 示例数据更新（可选）
-- 为现有的练习册添加示例图片
-- UPDATE exercise_book 
-- SET images = JSON_ARRAY(
--     JSON_OBJECT(
--         'url', 'https://example.com/images/math-cover.jpg',
--         'description', '数学练习册封面',
--         'type', 'cover',
--         'order', 0
--     )
-- ) 
-- WHERE subject = '数学' AND images IS NULL;

-- UPDATE exercise_book 
-- SET images = JSON_ARRAY(
--     JSON_OBJECT(
--         'url', 'https://example.com/images/chinese-cover.jpg',
--         'description', '语文练习册封面',
--         'type', 'cover',
--         'order', 0
--     )
-- ) 
-- WHERE subject = '语文' AND images IS NULL;

-- 添加约束检查（可选）
-- 确保images字段如果不为空，则必须是有效的JSON数组
-- ALTER TABLE exercise_book 
-- ADD CONSTRAINT chk_exercise_book_images 
-- CHECK (images IS NULL OR JSON_VALID(images));

-- 注释说明：
-- 1. images字段使用JSON类型存储图片信息数组
-- 2. 每个图片对象包含：url（必需）、description（可选）、type（可选）、order（可选）
-- 3. type字段用于区分图片类型：cover（封面）、content（内容图）等
-- 4. order字段用于排序显示
-- 5. 支持存储多个图片，满足一个或多个图片的需求
