# GSL - Godzilla Super Loader / Extension

GSL 是一个基于 Godzilla 开发的增强型 Shell 管理工具与插件集合，旨在提供更强大的后渗透能力，特别是在绕过 RASP (Runtime Application Self-Protection) 和内存马注入方面。

## 核心功能

### 1. RASP 绕过插件 (RaspBypass)
集成了多种先进的 RASP 绕过技术，支持自动探测并优选执行路径。
- **多种绕过方法**：
  - `Unsafe.allocateInstance` + `forkAndExec`：绕过构造函数监控。
  - **JNI 原生执行**：通过加载 Native 库执行命令，完全脱离 JVM 监控层。
  - **新线程/GC 触发**：利用 Java 机制切割调用堆栈，破坏 RASP 的上下文检测。
  - **Tomcat-JNI**：直接利用 Tomcat 内置的 JNI 库。
- **RASP 检测与禁用**：支持 OpenRASP, JRASP, Elkeid, 阿里/腾讯 RASP 等主流产品的检测与一键禁用 Hook 开关。

### 2. 内存马注入 (Memory Shell)
支持多种中间件和框架的内存马注入，具备极高的隐蔽性。
- **支持类型**：
  - Tomcat (Filter, Servlet, Listener)
  - Spring (Controller)
  - Jetty (Filter)
  - **VM Anonymous Class**：利用匿名类注入，规避常规内存马检测工具。

### 3. 多功能插件集
- **权限维持与提权**：集成 Mimikatz, EfsPotato, Useradd 等工具。
- **OA 专项利用**：针对金蝶、致远、泛微、万户等主流 OA 系统的专项利用模块。
- **扫描与探测**：内置 ShellAvscan 插件，用于探测目标机器上的安全防护软件。

### 4. 现代化 UI 与 交互
- 采用 **FlatLaf** 现代化 UI 框架，支持多种主题切换。
- 内置壁纸管理器与透明度调节，提供极致的使用体验。
- 完善的操作审计日志，记录每一步操作轨迹。

## 项目结构

- `bin/`: 存放编译好的 `gsl5.jar` 主程序。
- `src/`:
  - `core/`: 核心 UI 逻辑与配置管理。
  - `shells/`:
    - `payloads/`: 各平台动态载荷实现。
    - `cryptions/`: 流量加密算法实现。
    - `plugins/`: 各种增强功能插件。
- `native/`: RASP 绕过相关的 C/C++ 源码及编译脚本。

## 快速开始

1. **生成授权**：使用 `KeyGen.java` 生成对应的授权文件。
2. **启动程序**：运行 `bin/gsl5.jar`。
3. **配置 Shell**：添加目标 Shell，并在插件栏中选择功能模块。

## 编译指南

- **服务端模块**：运行 `compile_rasp_bypass.bat` 或 `.sh`。
- **JNI 库**：在 `native/` 目录下运行对应的 `build_jni` 脚本。

## 免责声明

本工具仅供安全研究和授权测试使用，严禁用于非法用途。使用本工具产生的任何后果由使用者自行承担。
