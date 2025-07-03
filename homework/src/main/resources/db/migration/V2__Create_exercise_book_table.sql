-- 创建练习册表
-- 用于存储练习册相关信息

CREATE TABLE IF NOT EXISTS exercise_book (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    title VARCHAR(200) NOT NULL COMMENT '练习册标题',
    description TEXT COMMENT '练习册描述',
    subject VARCHAR(50) COMMENT '学科',
    grade VARCHAR(20) COMMENT '年级',
    difficulty_level INT DEFAULT 1 COMMENT '难度等级 (1-5)',
    creator_id BIGINT COMMENT '创建者ID',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态 (ACTIVE, INACTIVE, DELETED)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_title (title),
    INDEX idx_subject (subject),
    INDEX idx_grade (grade),
    INDEX idx_creator_id (creator_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='练习册表';

-- 插入示例数据
INSERT INTO exercise_book (title, description, subject, grade, difficulty_level, creator_id, status) VALUES
('数学基础练习册', '小学三年级数学基础练习题集', '数学', '三年级', 2, 1, 'ACTIVE'),
('语文阅读理解', '四年级语文阅读理解专项练习', '语文', '四年级', 3, 1, 'ACTIVE'),
('英语单词练习', '五年级英语单词记忆练习册', '英语', '五年级', 2, 1, 'ACTIVE'),
('科学实验指南', '六年级科学实验操作指南', '科学', '六年级', 4, 1, 'ACTIVE'); 