# NetCore 完整载荷（NetCoreDynamicPayload）

## 目录

```
src/shells/payloads/netcore/     NetCoreShell + payload_core.dll
src/shells/cryptions/netcore/    NETCORE_AES_BASE64 + Middleware 模板
src/shells/plugins/netcore/      插件 UI 包装 + assets/*.dll
dotnet_core_payload/             C# 源码（载荷 + 插件）
```

## 名称

| 类型 | 名称 |
|------|------|
| Payload | `NetCoreDynamicPayload` |
| Cryption | `NETCORE_AES_BASE64` |
| 入口 | ASP.NET Core Middleware (`.cs`) |

## 内置能力（payload_core.dll / LY）

- 会话：`test` / `close`
- 信息：`getBasicsInfo`
- 文件：列表/读写/复制/移动/删除/新建/属性/大文件/远程下载
- 命令：`execCommand`（Windows cmd / *nix sh）
- SQL：`execSql`（需目标进程已加载对应 DbProviderFactory）
- 插件加载：`include` + 经典 `Equals(Hashtable)/ToString()` 调度

## 插件（plugins/netcore）

| 插件 | Display | 服务端 DLL |
|------|---------|------------|
| RealCmd | 虚拟终端 | RealCmd.dll |
| SuperTerminal | 超级终端 | （复用 RealCmd） |
| PortScan | 端口扫描 | CProtScan.dll |
| Zip | ZIP压缩 | CZip.dll |
| HttpProxy | Http代理 | HttpRequest.dll |
| EasySocksProxy | 轻量代理 | SocketManage.dll |
| InlineExecuteAssembly | ExecuteAssembly | ExecuteAssembly.dll |
| EvalCode | 代码执行 | EvalCode.dll（**推荐 Run DLL**；内联编译需 Roslyn） |
| ShellcodeLoader | Shellcode | ShellcodeLoader.dll（**仅 Windows**） |

未移植（强依赖 System.Web / 域提权）：MemoryShell、ListMachineKey、Potato 系列、SharpWeb、Mimikatz 等。

## 使用

1. 生成：Payload=`NetCoreDynamicPayload`，加密器=`NETCORE_AES_BASE64`
2. 将生成的 Middleware 放入 ASP.NET Core 项目：`app.UseMiddleware<GslCoreShellMiddleware>();`
3. 添加 Shell，密码密钥一致
4. 打开 Shell 后插件页可见上表功能

## 重编

```powershell
powershell -File dotnet_core_payload/Plugins/build-plugins.ps1
# 输出: dotnet_core_payload/Plugins/out/*.dll
# 再拷到 src/shells/payloads/netcore/assets 与 src/shells/plugins/netcore/assets
```
