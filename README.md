# oinsist-backend

仿照 [RuoYi-Vue-Plus](https://gitee.com/dromara/RuoYi-Vue-Plus) 核心架构思想，基于现代技术栈从零逆向精简重构的后端多模块工程，面向教学场景设计。

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 长期支持版本，支持虚拟线程、Switch 模式匹配 |
| Spring Boot | 3.5.14 | 全面遵循 Jakarta EE 规范 |
| PostgreSQL | 42.7.11 | 主数据库，主键采用雪花算法 |
| MyBatis-Plus | 3.5.16 | Spring Boot 3 适配版（Jakarta 命名空间） |
| Sa-Token | 1.45.0 | 轻量级权限认证框架 |
| Redisson | 4.4.0 | 分布式锁 / 限流 / 缓存 |
| Springdoc OpenAPI | 3.0.3 | 接口文档方案 |

## 模块结构

```
oinsist-backend
├── oinsist-admin          # 启动层 + Controller 出口（暴露 HTTP 接口）
├── oinsist-system         # RBAC 核心业务（用户、角色、部门、菜单）
├── oinsist-common         # 公共能力聚合层
│   ├── oinsist-common-core      # 基础常量、枚举、响应模型、通用异常、工具类
│   ├── oinsist-common-web       # 全局异常处理、WebMvc 配置、过滤器
│   ├── oinsist-common-mybatis   # MyBatis-Plus 配置、分页、数据权限、租户拦截器
│   ├── oinsist-common-redis     # Redis 缓存、Redisson 分布式锁
│   ├── oinsist-common-satoken   # Sa-Token 登录认证、权限校验
│   ├── oinsist-common-log       # 操作日志切面
│   └── oinsist-common-crypto    # 接口加解密
└── sql                    # 数据库初始化脚本
```

**依赖方向**：`admin` → `system` → `common-*`（严格自上而下，禁止反向依赖）

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- PostgreSQL 16+
- Redis 7+

### 本地运行

```bash
# 1. 克隆项目
git clone https://github.com/oinsist/oinsist-backend.git
cd oinsist-backend

# 2. 初始化数据库（执行 sql/ 目录下的脚本）

# 3. 修改配置文件
# oinsist-admin/src/main/resources/application.yml

# 4. 编译 & 启动
mvn clean install -DskipTests
mvn spring-boot:run -pl oinsist-admin
```

启动后访问接口文档：http://localhost:8080/doc.html

## 设计原则

1. **核心思想留存，杜绝过度封装** — 只实现最核心的主流程逻辑，保持极简高可读
2. **现代技术栈优先** — Java 21 + Spring Boot 3.5 + Jakarta EE，禁止过时 API
3. **教学友好** — 关键配置附带解释性中文注释，说明设计意图与架构决策

## License

MIT
