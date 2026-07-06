# GSL5 — Godzilla Super Loader 5

基于 Godzilla 深度二次开发的 WebShell 管理与红队后渗透平台。在保留 Godzilla 全部能力的基础上，扩展了 **MCP 服务（AI 驱动）**、**团队协作（多数据源）**、**RASP 绕过 + 多态混淆**、**内存马注入** 等能力。

> ⚠️ 本工具仅供**授权的安全测试与研究**使用，严禁用于非法用途。详见文末免责声明。

---

## 核心特性

### 1. 双运行模式
- **GUI 模式（默认）**：图形化操作界面，启动时可选择数据源
- **MCP 无头模式**：`java -jar gsl5.jar mcp [port]`，内置 HTTP/SSE 服务，供 Claude Desktop / Claude Code 等 AI 助手远程调用全部功能（Shell 增删改查、命令执行、文件操作、数据库、Payload 生成等 40+ 工具）

### 2. 数据源与团队协作
启动时由 `StartupModeDialog` 选择数据源：

| 模式 | 说明 |
|------|------|
| 单机模式 | 本地 SQLite `data.db` |
| 团队-远程 SQLite | UNC 共享路径（如 `\\host\share\data.db`） |
| 团队-PostgreSQL | 远程 PG，多人共享 Shell 列表 + 操作审计日志 |

- `core/Db.java`：SQLite 持久化层（shell / shellEnv / plugin / seting / shellGroup 五表）
- `core/MigrateDb.java`：SQLite → PostgreSQL 一键迁移工具

### 3. Shell 全功能管理
- 集中管理 **JSP / ASPX / PHP / ASP** 多类 WebShell
- 文件管理（`ShellFileManager`：大文件分片续传、远程下载、属性修改）、命令执行、数据库工具（MySQL/Oracle/SQLServer/PostgreSQL/SQLite）、提权
- 批量连接测试、搜索、克隆、导入导出（`gsl5://import?data=...`）

### 4. RASP 绕过与多态混淆（RaspBypass 插件）
- **命令执行绕过**：`Unsafe.allocateInstance` + `forkAndExec`、JNI 原生执行、新线程 / GC finalize、Tomcat-JNI、ProcessImpl 直调、反射关闭后执行
- **RASP 探测与禁用**：自动识别 OpenRASP / JRASP / Elkeid 等，支持禁用 Hook、改配置、卸载 Agent
- **ASM 多态混淆**：上传 `RaspBypassModule` 前，用 ObjectWeb ASM 注入随机字段名 + 随机 NOP 指令，使每次字节码特征不同，规避哈希 / 静态签名检测（见 `RaspBypass.obfuscateModule()`）
- **内存马注入**：Tomcat（Filter/Servlet/Listener）、Spring（Controller）、Jetty、VM Anonymous Class（隐蔽性最高）

### 5. 插件集
| 插件 | 平台 | 功能 |
|------|------|------|
| `TH_TOOLS` | Java/C# | Potato 系列提权、Shellcode 注入、自定义 Payload |
| `Mimikatz` | 通用 | 凭据抓取 |
| `Useradd` | Java/C#/通用 | 添加账号 |
| `OaTools` | Java/C# | 金蝶 / 致远 / 泛微 / 用友 / Weblogic / vCenter 等专项代理 |
| `ShellAvscan` | 通用 | 目标安全软件探测 |
| `RaspBypass` | Java | RASP 绕过 + 内存马（见上） |
| `McpService` | 通用 | 内置 MCP HTTP/SSE 服务，桥接 AI 助手 |

### 6. 现代化 UI 与审计
- FlatLaf 主题、壁纸管理器、透明度调节
- `OperationAuditLog` 全量操作审计：记录谁、何时、做了什么

---

## 项目结构

```
gsl/
├── src/
│   ├── core/            # 核心框架：UI、数据访问(Db/MigrateDb)、配置、C2 profile、操作审计
│   │   ├── Db.java              # SQLite 持久化层
│   │   ├── MigrateDb.java       # SQLite → PostgreSQL 迁移
│   │   └── ui/                  # MainActivity、StartupModeDialog、ShellFileManager、DataView 等
│   ├── shells/
│   │   ├── payloads/    # 各平台动态载荷（Java/C# 模块）
│   │   ├── cryptions/   # 流量加密（JavaAes/JavaC2、csharpAes、phpXor、aspXor）
│   │   └── plugins/     # 增强插件（java / csharp / generic）
│   ├── util/            # HTTP、IP 库、工具函数
│   └── data/            # 内置资源（av.json、META-INF）
├── native/              # RASP 绕过 JNI 源码（rasp_bypass_jni.c）+ build_jni 脚本
├── bin/gsl5.jar         # 预编译主程序
├── KeyGen.java          # 授权文件生成器（输出 license.lic）
├── compile_rasp_bypass.bat/.sh   # RASP 模块编译脚本
└── build_all_rasp_bypass.bat     # 一键全量编译
```

> 运行/构建还需外部依赖（`lib/`：ASM、PostgreSQL 驱动等；`bin/`：okhttp、kotlin-stdlib 等），未纳入本仓库。

---

## 快速开始

1. **生成授权**：编译并运行 `KeyGen.java`，生成 `license.lic` 放到运行目录
   ```
   javac KeyGen.java && java KeyGen
   ```
2. **启动 GUI**：
   ```
   java -jar bin/gsl5.jar
   ```
   启动后选择数据源（单机 SQLite / 远程 SQLite / PostgreSQL）。
3. **启动 MCP 无头模式**（AI 操控）：
   ```
   java -jar bin/gsl5.jar mcp          # 默认端口 9123
   java -jar bin/gsl5.jar mcp 9999     # 自定义端口
   ```
   然后在 Claude 的 MCP 配置中添加：
   ```json
   { "mcpServers": { "gsl5": { "type": "sse", "url": "http://127.0.0.1:9123/sse" } } }
   ```

---

## 编译指南

源码默认 **GBK** 编码（`McpService.java` 为 UTF-8）：

```bash
# 核心源码（GBK）
javac -encoding GBK -cp "lib/*;bin/*" -d out/production/gsl5 \
  src/core/**/*.java src/shells/**/*.java src/util/**/*.java

# MCP 服务（UTF-8）
javac -encoding UTF-8 -cp "lib/*;bin/*;out/production/gsl5" \
  -d out/production/gsl5 src/shells/plugins/generic/McpService.java

# RASP 服务端模块
compile_rasp_bypass.bat        # Windows
./compile_rasp_bypass.sh       # Linux/macOS

# JNI 库（可选，自定义原生绕过）
cd native && build_jni.bat win-x64   # 或 build_jni.sh linux-x64

# 打包
cd out/production/gsl5 && jar cf gsl5.jar *
```

更新已有 jar 中的单个 class：`jar uf gsl5.jar shells/plugins/generic/McpService.class`

---

## 配置文件

| 文件 | 说明 |
|------|------|
| `license.lic` | 授权文件，格式 `GSL1:<AES-CBC 密文>`，由 `KeyGen.java` 生成，可绑定目录 + 有效期 |
| `data.db` | SQLite 数据库，存储 Shell 配置 / 插件 / 环境变量 / 设置 |
| `config.yaml` | 运行配置（MCP 端口、认证、团队 PG 连接等） |

---

## 免责声明

本工具仅供**安全研究和授权测试**使用，严禁用于任何非法用途。使用本工具产生的任何后果由使用者自行承担。操作会被审计日志完整记录。
