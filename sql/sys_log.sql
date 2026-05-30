-- =============================================
-- 操作日志 & 登录日志表结构
-- 适配 PostgreSQL 16+，主键采用雪花算法
-- =============================================

-- ========================================
-- 操作日志表
-- ========================================
CREATE TABLE IF NOT EXISTS sys_oper_log (
    oper_id        BIGINT       PRIMARY KEY,
    title          VARCHAR(50)  DEFAULT '',
    business_type  INTEGER      DEFAULT 0,
    method         VARCHAR(200) DEFAULT '',
    request_method VARCHAR(10)  DEFAULT '',
    request_url    VARCHAR(500) DEFAULT '',
    request_param  TEXT,
    status         INTEGER      DEFAULT 0,
    error_msg      VARCHAR(2000) DEFAULT '',
    user_id        BIGINT,
    username       VARCHAR(64)  DEFAULT '',
    ip             VARCHAR(128) DEFAULT '',
    duration       BIGINT       DEFAULT 0,
    oper_time      TIMESTAMP    NOT NULL,
    deleted        INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_oper_log IS '操作日志表';
COMMENT ON COLUMN sys_oper_log.oper_id IS '日志主键（雪花算法）';
COMMENT ON COLUMN sys_oper_log.title IS '模块标题';
COMMENT ON COLUMN sys_oper_log.business_type IS '业务类型（0=其他 1=新增 2=修改 3=删除 4=导出 5=导入 6=授权 7=强退）';
COMMENT ON COLUMN sys_oper_log.method IS '方法名称';
COMMENT ON COLUMN sys_oper_log.request_method IS '请求方式（GET/POST/PUT/DELETE）';
COMMENT ON COLUMN sys_oper_log.request_url IS '请求URL';
COMMENT ON COLUMN sys_oper_log.request_param IS '请求参数（已脱敏）';
COMMENT ON COLUMN sys_oper_log.status IS '操作状态（0=成功 1=异常）';
COMMENT ON COLUMN sys_oper_log.error_msg IS '错误消息';
COMMENT ON COLUMN sys_oper_log.user_id IS '操作人ID';
COMMENT ON COLUMN sys_oper_log.username IS '操作人用户名';
COMMENT ON COLUMN sys_oper_log.ip IS '操作IP地址';
COMMENT ON COLUMN sys_oper_log.duration IS '执行耗时（毫秒）';
COMMENT ON COLUMN sys_oper_log.oper_time IS '操作时间';
COMMENT ON COLUMN sys_oper_log.deleted IS '逻辑删除标识（0=未删除 1=已删除）';

-- ========================================
-- 登录日志表
-- ========================================
CREATE TABLE IF NOT EXISTS sys_login_log (
    login_id   BIGINT       PRIMARY KEY,
    user_id    BIGINT,
    username   VARCHAR(64)  NOT NULL,
    status     INTEGER      DEFAULT 0,
    ip         VARCHAR(128) DEFAULT '',
    user_agent VARCHAR(500) DEFAULT '',
    msg        VARCHAR(500) DEFAULT '',
    login_time TIMESTAMP    NOT NULL,
    deleted    INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_login_log IS '登录日志表';
COMMENT ON COLUMN sys_login_log.login_id IS '日志主键（雪花算法）';
COMMENT ON COLUMN sys_login_log.user_id IS '用户ID（登录成功时记录）';
COMMENT ON COLUMN sys_login_log.username IS '登录账号';
COMMENT ON COLUMN sys_login_log.status IS '登录状态（0=成功 1=失败）';
COMMENT ON COLUMN sys_login_log.ip IS '登录IP';
COMMENT ON COLUMN sys_login_log.user_agent IS '浏览器User-Agent';
COMMENT ON COLUMN sys_login_log.msg IS '提示消息（如：登录成功、密码错误）';
COMMENT ON COLUMN sys_login_log.login_time IS '登录时间';
COMMENT ON COLUMN sys_login_log.deleted IS '逻辑删除标识（0=未删除 1=已删除）';

-- =============================================
-- 日志管理菜单与权限初始化数据
-- 对应 Controller 权限点：system:operLog:list/remove、system:loginLog:list/remove
-- =============================================

-- 操作日志（菜单 C，挂在"系统管理"目录下，parent_id=1）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(103, '操作日志', 1, 4, 'operlog', 'system/operlog/index', 'system:operLog:list', 'C', '0', '0', 'form', NOW()) ON CONFLICT DO NOTHING;
-- 登录日志（菜单 C）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(104, '登录日志', 1, 5, 'loginlog', 'system/loginlog/index', 'system:loginLog:list', 'C', '0', '0', 'logininfor', NOW()) ON CONFLICT DO NOTHING;

-- 操作日志按钮权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(1030, '操作日志查询', 103, 1, '', '', 'system:operLog:list', 'F', '0', '0', '', NOW()) ON CONFLICT DO NOTHING;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(1031, '操作日志删除', 103, 2, '', '', 'system:operLog:remove', 'F', '0', '0', '', NOW()) ON CONFLICT DO NOTHING;

-- 登录日志按钮权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(1040, '登录日志查询', 104, 1, '', '', 'system:loginLog:list', 'F', '0', '0', '', NOW()) ON CONFLICT DO NOTHING;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(1041, '登录日志删除', 104, 2, '', '', 'system:loginLog:remove', 'F', '0', '0', '', NOW()) ON CONFLICT DO NOTHING;

-- 角色菜单关联（admin 角色 role_id=1 拥有日志管理全部权限）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 103) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 104) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 1030) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 1031) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 1040) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 1041) ON CONFLICT DO NOTHING;
