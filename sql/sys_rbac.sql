-- =============================================
-- P06 RBAC 核心表结构与初始化数据
-- 适配 PostgreSQL 16+，主键采用雪花算法
-- =============================================

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    user_id      BIGINT       PRIMARY KEY,
    username     VARCHAR(64)  NOT NULL UNIQUE,
    nickname     VARCHAR(64)  NOT NULL DEFAULT '',
    password     VARCHAR(200) NOT NULL DEFAULT '',
    status       VARCHAR(1)   NOT NULL DEFAULT '0',
    create_by    BIGINT,
    create_time  TIMESTAMP,
    update_by    BIGINT,
    update_time  TIMESTAMP,
    deleted      INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_user IS '用户表';
COMMENT ON COLUMN sys_user.user_id IS '用户ID（雪花算法）';
COMMENT ON COLUMN sys_user.username IS '用户账号';
COMMENT ON COLUMN sys_user.nickname IS '用户昵称';
COMMENT ON COLUMN sys_user.password IS '密码（BCrypt加密）';
COMMENT ON COLUMN sys_user.status IS '状态（0正常 1停用）';
COMMENT ON COLUMN sys_user.create_by IS '创建者';
COMMENT ON COLUMN sys_user.create_time IS '创建时间';
COMMENT ON COLUMN sys_user.update_by IS '更新者';
COMMENT ON COLUMN sys_user.update_time IS '更新时间';
COMMENT ON COLUMN sys_user.deleted IS '逻辑删除（0=未删除 1=已删除）';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    role_id      BIGINT       PRIMARY KEY,
    role_name    VARCHAR(64)  NOT NULL DEFAULT '',
    role_key     VARCHAR(100) NOT NULL DEFAULT '',
    status       VARCHAR(1)   NOT NULL DEFAULT '0',
    create_by    BIGINT,
    create_time  TIMESTAMP,
    update_by    BIGINT,
    update_time  TIMESTAMP,
    deleted      INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_role IS '角色表';
COMMENT ON COLUMN sys_role.role_id IS '角色ID（雪花算法）';
COMMENT ON COLUMN sys_role.role_name IS '角色名称';
COMMENT ON COLUMN sys_role.role_key IS '角色标识（如 admin、common）';
COMMENT ON COLUMN sys_role.status IS '状态（0正常 1停用）';
COMMENT ON COLUMN sys_role.create_by IS '创建者';
COMMENT ON COLUMN sys_role.create_time IS '创建时间';
COMMENT ON COLUMN sys_role.update_by IS '更新者';
COMMENT ON COLUMN sys_role.update_time IS '更新时间';
COMMENT ON COLUMN sys_role.deleted IS '逻辑删除（0=未删除 1=已删除）';

-- 菜单表
CREATE TABLE IF NOT EXISTS sys_menu (
    menu_id      BIGINT       PRIMARY KEY,
    menu_name    VARCHAR(64)  NOT NULL DEFAULT '',
    parent_id    BIGINT       NOT NULL DEFAULT 0,
    order_num    INTEGER      NOT NULL DEFAULT 0,
    path         VARCHAR(200) NOT NULL DEFAULT '',
    component    VARCHAR(255) NOT NULL DEFAULT '',
    perms        VARCHAR(200) NOT NULL DEFAULT '',
    menu_type    VARCHAR(1)   NOT NULL DEFAULT '',
    visible      VARCHAR(1)   NOT NULL DEFAULT '0',
    status       VARCHAR(1)   NOT NULL DEFAULT '0',
    icon         VARCHAR(100) NOT NULL DEFAULT '',
    create_by    BIGINT,
    create_time  TIMESTAMP,
    update_by    BIGINT,
    update_time  TIMESTAMP,
    deleted      INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_menu IS '菜单权限表';
COMMENT ON COLUMN sys_menu.menu_id IS '菜单ID（雪花算法）';
COMMENT ON COLUMN sys_menu.menu_name IS '菜单名称';
COMMENT ON COLUMN sys_menu.parent_id IS '父菜单ID（0表示顶级）';
COMMENT ON COLUMN sys_menu.order_num IS '显示顺序';
COMMENT ON COLUMN sys_menu.path IS '路由地址';
COMMENT ON COLUMN sys_menu.component IS '组件路径';
COMMENT ON COLUMN sys_menu.perms IS '权限标识（如 system:user:list）';
COMMENT ON COLUMN sys_menu.menu_type IS '菜单类型（M目录 C菜单 F按钮）';
COMMENT ON COLUMN sys_menu.visible IS '是否可见（0可见 1隐藏）';
COMMENT ON COLUMN sys_menu.status IS '状态（0正常 1停用）';
COMMENT ON COLUMN sys_menu.icon IS '菜单图标';
COMMENT ON COLUMN sys_menu.create_by IS '创建者';
COMMENT ON COLUMN sys_menu.create_time IS '创建时间';
COMMENT ON COLUMN sys_menu.update_by IS '更新者';
COMMENT ON COLUMN sys_menu.update_time IS '更新时间';
COMMENT ON COLUMN sys_menu.deleted IS '逻辑删除（0=未删除 1=已删除）';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

COMMENT ON TABLE sys_user_role IS '用户角色关联表';
COMMENT ON COLUMN sys_user_role.user_id IS '用户ID';
COMMENT ON COLUMN sys_user_role.role_id IS '角色ID';

-- 角色菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);

COMMENT ON TABLE sys_role_menu IS '角色菜单关联表';
COMMENT ON COLUMN sys_role_menu.role_id IS '角色ID';
COMMENT ON COLUMN sys_role_menu.menu_id IS '菜单ID';

-- =============================================
-- 初始化数据
-- =============================================

-- 初始用户（密码均为 BCrypt 加密）
-- admin/admin123
INSERT INTO sys_user (user_id, username, nickname, password, status, create_time) VALUES
(1, 'admin', '管理员', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', NOW()) ON CONFLICT DO NOTHING;
-- test/test123
INSERT INTO sys_user (user_id, username, nickname, password, status, create_time) VALUES
(2, 'test', '测试用户', '$2a$10$BaHoMJFbJxMQHCafzczrx.jApiGbOjsumaIVMQmlfpPme/YhR3Iny', '0', NOW()) ON CONFLICT DO NOTHING;

-- 初始角色
INSERT INTO sys_role (role_id, role_name, role_key, status, create_time) VALUES
(1, '超级管理员', 'admin', '0', NOW()) ON CONFLICT DO NOTHING;
INSERT INTO sys_role (role_id, role_name, role_key, status, create_time) VALUES
(2, '普通角色', 'common', '0', NOW()) ON CONFLICT DO NOTHING;

-- 初始菜单
-- 系统管理（目录）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(1, '系统管理', 0, 1, 'system', '', '', 'M', '0', '0', 'system', NOW()) ON CONFLICT DO NOTHING;
-- 用户管理（菜单）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(100, '用户管理', 1, 1, 'user', 'system/user/index', 'system:user:list', 'C', '0', '0', 'user', NOW()) ON CONFLICT DO NOTHING;
-- 角色管理（菜单）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(101, '角色管理', 1, 2, 'role', 'system/role/index', 'system:role:list', 'C', '0', '0', 'peoples', NOW()) ON CONFLICT DO NOTHING;
-- 菜单管理（菜单）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(102, '菜单管理', 1, 3, 'menu', 'system/menu/index', 'system:menu:list', 'C', '0', '0', 'tree-table', NOW()) ON CONFLICT DO NOTHING;
-- 用户管理按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(1000, '用户查询', 100, 1, '', '', 'system:user:query', 'F', '0', '0', '', NOW()) ON CONFLICT DO NOTHING;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(1001, '用户新增', 100, 2, '', '', 'system:user:add', 'F', '0', '0', '', NOW()) ON CONFLICT DO NOTHING;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(1002, '用户修改', 100, 3, '', '', 'system:user:edit', 'F', '0', '0', '', NOW()) ON CONFLICT DO NOTHING;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(1003, '用户删除', 100, 4, '', '', 'system:user:remove', 'F', '0', '0', '', NOW()) ON CONFLICT DO NOTHING;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(1004, '分配角色', 100, 5, '', '', 'system:user:assignRole', 'F', '0', '0', '', NOW()) ON CONFLICT DO NOTHING;
-- 角色管理按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(1005, '角色查询', 101, 1, '', '', 'system:role:query', 'F', '0', '0', '', NOW()) ON CONFLICT DO NOTHING;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(1006, '角色新增', 101, 2, '', '', 'system:role:add', 'F', '0', '0', '', NOW()) ON CONFLICT DO NOTHING;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(1007, '角色修改', 101, 3, '', '', 'system:role:edit', 'F', '0', '0', '', NOW()) ON CONFLICT DO NOTHING;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(1008, '角色删除', 101, 4, '', '', 'system:role:remove', 'F', '0', '0', '', NOW()) ON CONFLICT DO NOTHING;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(1009, '分配菜单', 101, 5, '', '', 'system:role:assignMenu', 'F', '0', '0', '', NOW()) ON CONFLICT DO NOTHING;
-- 菜单管理按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(1010, '菜单查询', 102, 1, '', '', 'system:menu:query', 'F', '0', '0', '', NOW()) ON CONFLICT DO NOTHING;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(1011, '菜单新增', 102, 2, '', '', 'system:menu:add', 'F', '0', '0', '', NOW()) ON CONFLICT DO NOTHING;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(1012, '菜单修改', 102, 3, '', '', 'system:menu:edit', 'F', '0', '0', '', NOW()) ON CONFLICT DO NOTHING;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, icon, create_time) VALUES
(1013, '菜单删除', 102, 4, '', '', 'system:menu:remove', 'F', '0', '0', '', NOW()) ON CONFLICT DO NOTHING;

-- 用户角色关联
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1) ON CONFLICT DO NOTHING;
INSERT INTO sys_user_role (user_id, role_id) VALUES (2, 2) ON CONFLICT DO NOTHING;

-- 角色菜单关联（admin 拥有所有菜单，common 仅有用户查询）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 1) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 100) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 101) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 102) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 1000) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 1001) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 1002) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 1003) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 1004) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 1005) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 1006) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 1007) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 1008) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 1009) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 1010) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 1011) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 1012) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 1013) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 1) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 100) ON CONFLICT DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 1000) ON CONFLICT DO NOTHING;
