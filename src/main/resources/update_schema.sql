-- 为现有工单表添加 ai_answer 字段
ALTER TABLE sys_ticket ADD COLUMN IF NOT EXISTS ai_answer TEXT COMMENT 'AI回答内容' AFTER question;
