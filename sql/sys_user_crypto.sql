-- P10 数据安全：为 sys_user 添加加密字段
-- 说明：email 和 phonenumber 存储 AES-GCM 密文（Base64 编码），
--       长度预留 256 字符以容纳加密后的膨胀（明文 + IV + AuthTag + Base64 编码）

ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS email VARCHAR(256) DEFAULT NULL;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS phonenumber VARCHAR(256) DEFAULT NULL;

COMMENT ON COLUMN sys_user.email IS '邮箱（AES-GCM 加密存储）';
COMMENT ON COLUMN sys_user.phonenumber IS '手机号码（AES-GCM 加密存储）';
