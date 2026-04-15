# RASP Bypass Plugin for Godzilla

一个功能强大的RASP绕过插件，集成多种绕过技术，支持主流RASP产品。

## 功能特性

### 1. 命令执行绕过
- **Unsafe.allocateInstance** - 绕过UNIXProcess构造函数检测
- **JNI Native Execution** - 通过JNI执行Native代码
- **New Thread Bypass** - 新建线程绕过上下文检测
- **GC Finalize Bypass** - 通过GC触发执行
- **ProcessImpl Direct** - 直接调用ProcessImpl
- **Tomcat-JNI** - 利用Tomcat内置JNI库
- **Reflection Bypass** - 反射关闭RASP后执行
- **ForkAndExec Direct** - 直接调用forkAndExec

### 2. RASP检测与禁用
- 自动检测OpenRASP、JRASP、Elkeid等主流RASP
- 支持多种禁用方式：
  - 禁用Hook开关
  - 修改配置值
  - 卸载RASP Agent

### 3. 内存马注入
- Tomcat Filter内存马
- Tomcat Servlet内存马
- Spring Controller内存马
- VM Anonymous Class内存马（隐蔽性最高）

### 4. 辅助工具
- 复制二进制文件绕过黑名单
- 创建符号链接
- JNI库加载与管理

## 编译

### 编译服务端模块

Windows:
```batch
compile_rasp_bypass.bat
```

Linux/macOS:
```bash
chmod +x compile_rasp_bypass.sh
./compile_rasp_bypass.sh
```

### 编译JNI库（可选）

如果需要使用自定义JNI绕过：

Linux:
```bash
cd native
chmod +x build_jni.sh
./build_jni.sh linux-x64
```

Windows:
```batch
cd native
build_jni.bat win-x64
```

## 使用方法

### 1. 安装插件
将编译好的文件放入Godzilla插件目录：
- `RaspBypass.java` - 客户端插件
- `assets/RaspBypassModule.classs` - 服务端模块

### 2. 加载插件
在Godzilla中选择目标Shell，右键 -> 插件 -> RASP绕过

### 3. 命令执行
1. 选择绕过方法（推荐Auto Detect）
2. 输入命令
3. 点击Execute

### 4. 禁用RASP
1. 切换到RASP Disable标签
2. 选择RASP类型
3. 选择禁用方式
4. 点击Check RASP Status检查状态
5. 点击Disable RASP执行禁用

### 5. 注入内存马
1. 切换到Memory Shell标签
2. 选择内存马类型
3. 输入URL路径
4. 点击Inject Memory Shell

## 绕过技术详解

### 1. Unsafe.allocateInstance
```java
// 原理：绕过构造函数检测
Unsafe unsafe = getUnsafe();
Object process = unsafe.allocateInstance(UNIXProcess.class);
// 直接调用forkAndExec
```

### 2. JNI绕过
```java
// 原理：RASP无法监控Native层
System.load("/tmp/evil.so");
nativeExec("whoami");
```

### 3. 新线程绕过
```java
// 原理：切割堆栈破坏上下文
new Thread(() -> {
    Runtime.getRuntime().exec(cmd);
}).start();
```

### 4. GC触发
```java
// 原理：在finalize中执行
Object obj = new Object() {
    protected void finalize() {
        Runtime.getRuntime().exec(cmd);
    }
};
obj = null;
System.gc();
```

### 5. Tomcat-JNI
```java
// 原理：利用Tomcat内置JNI库
Library.initialize(null);
Proc.create(proc, "/bin/sh", args, env, procattr, pool);
```

## 支持的RASP产品

| RASP产品 | 检测 | 禁用Hook | 修改配置 | 卸载 |
|----------|------|----------|----------|------|
| OpenRASP | ✅ | ✅ | ✅ | ⚠️ |
| JRASP | ✅ | ✅ | ❌ | ⚠️ |
| Elkeid | ✅ | ❌ | ❌ | ❌ |
| 自定义 | ⚠️ | ⚠️ | ⚠️ | ⚠️ |

⚠️ = 部分支持/需要特定条件

## 注意事项

1. **权限要求**：部分功能需要高权限
2. **兼容性**：不同JDK版本可能有差异
3. **检测规避**：建议先检测RASP再选择绕过方法
4. **日志清理**：操作后记得清理日志

## 文件结构

```
src/shells/plugins/java/
├── RaspBypass.java              # 客户端插件
└── assets/
    ├── RaspBypassModule.java    # 服务端模块源码
    └── RaspBypassModule.classs  # 编译后的模块

native/
├── rasp_bypass_jni.c            # JNI C代码
├── build_jni.sh                 # Linux编译脚本
└── build_jni.bat                # Windows编译脚本
```

## 参考资料

- [RASP攻防下的黑魔法 - Kcon 2022](https://g1asssy.com/2023/06/11/rasp_tricks/)
- [绕RASP之内存马后渗透浅析](https://mp.weixin.qq.com/s/SK_ZBqrbxBBOs2zefRE4vw)
- [RASP的安全攻防研究实践](https://www.cnblogs.com/wh4am1/p/16780056.html)
- [Java内存攻击技术漫谈](https://xz.aliyun.com/t/10075)

## 免责声明

本工具仅供安全研究和授权测试使用，请勿用于非法用途。使用本工具产生的任何后果由使用者自行承担。
