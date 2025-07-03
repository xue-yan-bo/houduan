-- ===================================================================
-- 作业表创建脚本
-- 用于Spring Data JPA + MySQL
-- ===================================================================

-- 创建作业表
CREATE TABLE IF NOT EXISTS homework (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    title VARCHAR(200) NOT NULL COMMENT '作业标题',
    description TEXT COMMENT '作业描述',
    subject VARCHAR(50) COMMENT '科目',
    teacher_id BIGINT COMMENT '教师ID',
    class_id BIGINT COMMENT '班级ID',
    due_date DATETIME COMMENT '截止日期',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT-草稿，PUBLISHED-已发布，COMPLETED-已完成，EXPIRED-已过期',
    max_score INT DEFAULT 100 COMMENT '最高分数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(50) COMMENT '创建人',
    updated_by VARCHAR(50) COMMENT '更新人',
    
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_class_id (class_id),
    INDEX idx_status (status),
    INDEX idx_subject (subject),
    INDEX idx_due_date (due_date),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业表';

-- 插入示例数据
INSERT INTO homework (title, description, subject, teacher_id, class_id, due_date, status, max_score, created_by) VALUES
('数学作业 - 第一章练习', '完成教材第一章的所有练习题，包括基础题和提高题。', '数学', 1001, 2001, '2024-01-15 23:59:59', 'PUBLISHED', 100, 'teacher_zhang'),
('语文作业 - 古诗词背诵', '背诵指定的10首古诗词，下节课进行默写检查。', '语文', 1002, 2001, '2024-01-12 08:00:00', 'PUBLISHED', 50, 'teacher_li'),
('英语作业 - 单词记忆', '记忆Unit 1-3的所有单词，准备下周的单词测试。', '英语', 1003, 2002, '2024-01-18 10:00:00', 'DRAFT', 80, 'teacher_wang'),
('物理作业 - 实验报告', '完成光学实验的实验报告，包括实验过程和结论分析。', '物理', 1004, 2003, '2024-01-20 17:00:00', 'PUBLISHED', 120, 'teacher_zhao');
