-- 系统配置表（P03 验证用）
CREATE TABLE IF NOT EXISTS sys_config (
    config_id    BIGINT       PRIMARY KEY,
    config_name  VARCHAR(100) NOT NULL DEFAULT '',
    config_key   VARCHAR(100) NOT NULL DEFAULT '',
    config_value VARCHAR(500) NOT NULL DEFAULT '',
    create_by    BIGINT,
    create_time  TIMESTAMP,
    update_by    BIGINT,
    update_time  TIMESTAMP,
    deleted      INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_config IS '系统配置表';
COMMENT ON COLUMN sys_config.config_id IS '配置ID（雪花算法）';
COMMENT ON COLUMN sys_config.config_name IS '配置名称';
COMMENT ON COLUMN sys_config.config_key IS '配置键';
COMMENT ON COLUMN sys_config.config_value IS '配置值';
COMMENT ON COLUMN sys_config.create_by IS '创建者';
COMMENT ON COLUMN sys_config.create_time IS '创建时间';
COMMENT ON COLUMN sys_config.update_by IS '更新者';
COMMENT ON COLUMN sys_config.update_time IS '更新时间';
COMMENT ON COLUMN sys_config.deleted IS '逻辑删除（0=未删除 1=已删除）';
