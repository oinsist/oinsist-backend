-- ============================================================
-- 多租户支持 - 数据库迁移脚本
-- 策略：共享库共享表 + tenant_id 行级隔离
-- 全局共享表（不加 tenant_id）：sys_menu, sys_role_menu
-- 租户隔离表：所有其他业务表
-- ============================================================

-- ============================================================
-- 1. 创建租户主表
-- ============================================================

-- 租户表：记录系统中的所有租户信息，主键使用雪花算法生成
CREATE TABLE IF NOT EXISTS sys_tenant (
    tenant_id   BIGINT       PRIMARY KEY,             -- 租户ID（雪花算法）
    tenant_name VARCHAR(100) NOT NULL,                -- 租户名称
    contact     VARCHAR(50),                          -- 联系人
    status      CHAR(1)      NOT NULL DEFAULT '0',    -- 状态（0正常 1停用）
    create_by   BIGINT,                               -- 创建者
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_by   BIGINT,                               -- 更新者
    update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP, -- 更新时间
    deleted     INTEGER      DEFAULT 0                -- 逻辑删除标志（0正常 1删除）
);

COMMENT ON TABLE sys_tenant IS '租户表';
COMMENT ON COLUMN sys_tenant.tenant_id IS '租户ID（雪花算法）';
COMMENT ON COLUMN sys_tenant.tenant_name IS '租户名称';
COMMENT ON COLUMN sys_tenant.contact IS '联系人';
COMMENT ON COLUMN sys_tenant.status IS '状态（0正常 1停用）';
COMMENT ON COLUMN sys_tenant.create_by IS '创建者';
COMMENT ON COLUMN sys_tenant.create_time IS '创建时间';
COMMENT ON COLUMN sys_tenant.update_by IS '更新者';
COMMENT ON COLUMN sys_tenant.update_time IS '更新时间';
COMMENT ON COLUMN sys_tenant.deleted IS '逻辑删除标志（0正常 1删除）';

-- ============================================================
-- 2. 为租户隔离表添加 tenant_id 字段
--    使用 DEFAULT 1 使现有数据自动归属默认租户，无需额外 UPDATE
-- ============================================================

-- 用户表添加租户字段
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;

-- 角色表添加租户字段
ALTER TABLE sys_role ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;

-- 部门表添加租户字段
ALTER TABLE sys_dept ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;

-- 系统配置表添加租户字段
ALTER TABLE sys_config ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;

-- 用户角色关联表添加租户字段
ALTER TABLE sys_user_role ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;

-- 角色部门关联表添加租户字段
ALTER TABLE sys_role_dept ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;

-- 操作日志表添加租户字段
ALTER TABLE sys_oper_log ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;

-- 登录日志表添加租户字段
ALTER TABLE sys_login_log ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;

-- ============================================================
-- 设计决策：sys_role_menu 不添加 tenant_id
-- 原因：sys_role_menu 是角色-菜单权限关联表，跟随 sys_menu 采用全局共享策略。
-- 前提不变量：role_id 在全局范围内唯一（由 sys_role 的雪花算法主键保证），
-- 因此 sys_role_menu 中的 role_id 可以安全地关联到唯一的租户角色，
-- 不需要额外的 tenant_id 来区分归属。
-- ============================================================

-- ============================================================
-- 3. 删除旧的全局唯一约束 & 创建租户维度复合索引
--    使用 IF NOT EXISTS / IF EXISTS 保证幂等性
-- ============================================================

-- 删除旧的全局唯一约束（多租户下改为同租户内唯一）
-- sys_user 的 username 唯一性改由 (username, tenant_id) 部分索引保证
ALTER TABLE sys_user DROP CONSTRAINT IF EXISTS sys_user_username_key;
DROP INDEX IF EXISTS sys_user_username_key;

-- sys_user: 租户普通索引
CREATE INDEX IF NOT EXISTS idx_sys_user_tenant ON sys_user(tenant_id);

-- sys_user: 同一租户内用户名唯一（仅未删除记录参与约束，PostgreSQL 部分索引）
CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_user_username_tenant ON sys_user(username, tenant_id) WHERE deleted = 0;

-- sys_role: 租户普通索引
CREATE INDEX IF NOT EXISTS idx_sys_role_tenant ON sys_role(tenant_id);

-- sys_role: 同一租户内角色标识唯一（仅未删除记录参与约束）
CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_role_key_tenant ON sys_role(role_key, tenant_id) WHERE deleted = 0;

-- sys_dept: 租户普通索引
CREATE INDEX IF NOT EXISTS idx_sys_dept_tenant ON sys_dept(tenant_id);

-- sys_config: 租户普通索引
CREATE INDEX IF NOT EXISTS idx_sys_config_tenant ON sys_config(tenant_id);

-- sys_user_role: 租户普通索引
CREATE INDEX IF NOT EXISTS idx_sys_user_role_tenant ON sys_user_role(tenant_id);

-- sys_role_dept: 租户普通索引
CREATE INDEX IF NOT EXISTS idx_sys_role_dept_tenant ON sys_role_dept(tenant_id);

-- sys_oper_log: 租户普通索引
CREATE INDEX IF NOT EXISTS idx_sys_oper_log_tenant ON sys_oper_log(tenant_id);

-- sys_login_log: 租户普通索引
CREATE INDEX IF NOT EXISTS idx_sys_login_log_tenant ON sys_login_log(tenant_id);

-- ============================================================
-- 4. 插入默认租户数据（幂等：仅在不存在时插入）
-- ============================================================

-- 默认租户：tenant_id = 1，所有现有数据归属此租户
INSERT INTO sys_tenant (tenant_id, tenant_name, contact, status)
VALUES (1, '默认租户', '系统管理员', '0')
ON CONFLICT (tenant_id) DO NOTHING;
