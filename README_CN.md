# Skill Manager 技能同步工具

一个跨平台的技能同步工具，用于从多个 Git 仓库同步技能文件到目标目录，同时提供 PowerShell 和 Python 版本。

## 功能特性

- **多仓库支持**：从多个 Git 仓库同步技能
- **配置驱动**：使用 JSON 配置文件进行批量同步
- **CSV 管理**：通过 CSV 文件轻松管理技能，自动转换为 JSON
- **Git 集成**：每次同步前自动执行 `git pull`
- **Robocopy 驱动**：使用镜像模式进行可靠的文件复制
- **编码自动检测**：自动将 CSV 文件转换为 UTF-8 编码
- **详细日志**：完整的日志记录，包含目录树验证
- **批量操作**：通过配置文件顺序执行多个操作（初始化和同步）

## 文件结构

```
skill-manager/
├── powershell/          # PowerShell 版本（仅 Windows）
│   ├── sync-skills.ps1  # 主同步脚本
│   └── csv-to-json.ps1  # CSV 转 JSON 配置工具
├── python/              # Python 版本（跨平台）
│   ├── sync-skills.py   # 主同步脚本
│   └── csv-to-json.py   # CSV 转 JSON 配置工具
├── java/                # Java 版本（跨平台）
│   ├── src/main/java/com/skillmanager/
│   │   ├── CsvToJson.java       # CSV 转 JSON 配置工具
│   │   ├── SyncSkills.java       # 主同步脚本
│   │   ├── InitSkillRepo.java    # 仓库初始化脚本
│   │   └── BatchOperations.java  # 批量操作脚本
│   ├── pom.xml          # Maven 配置
│   ├── build.bat         # Windows 构建脚本
│   └── build.sh         # Linux/macOS 构建脚本
├── skills.csv           # 技能列表（CSV 格式）
├── sync-config.json     # 同步配置（JSON 格式）
├── batch-config.json    # 批量操作配置（JSON 格式）
├── README.md            # 英文说明文档
└── README_CN.md         # 中文说明文档
```

## 使用方法

### 1. 准备 CSV 配置

编辑 `skills.csv` 添加你的技能：

```csv
repoPath,skillName,description
E:\path\to\repo1,skill-name-1,技能描述 1
E:\path\to\repo1,skill-name-2,技能描述 2
E:\path\to\repo2,skill-name-3,技能描述 3
```

### 2. 转换 CSV 为 JSON

#### PowerShell 版本（仅 Windows）：

```powershell
.\powershell\csv-to-json.ps1
```

或使用自定义参数：

```powershell
.\powershell\csv-to-json.ps1 -CsvFile ".\skills.csv" -JsonFile ".\sync-config.json" -TargetPath "C:\Users\你的用户名\.trae-cn\skills\"
```

#### Python 版本（跨平台）：

```bash
python python/csv-to-json.py
```

或使用自定义参数：

```bash
python python/csv-to-json.py -CsvFile "skills.csv" -JsonFile "sync-config.json" -TargetPath "C:\Users\你的用户名\.trae-cn\skills\"
```

#### Java 版本（跨平台）：

首先构建项目：

```bash
# Windows
java/build.bat

# Linux/macOS
chmod +x java/build.sh
./java/build.sh
```

然后运行 CSV 转 JSON 工具：

```bash
java -jar java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar
```

或使用自定义参数：

```bash
java -jar java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar -CsvFile "skills.csv" -JsonFile "sync-config.json" -TargetPath "C:\Users\你的用户名\.trae-cn\skills\"
```

### 3. 同步技能

#### PowerShell 版本（仅 Windows）：

从配置文件同步所有技能：

```powershell
.\powershell\sync-skills.ps1 -ConfigFile ".\sync-config.json"
```

同步单个技能：

```powershell
.\powershell\sync-skills.ps1 -SkillName "skill-name" -RepoPath "E:\path\to\repo" -TargetPath "C:\Users\你的用户名\.trae-cn\skills\"
```

同步仓库中的所有技能：

```powershell
.\powershell\sync-skills.ps1 -RepoPath "E:\path\to\repo" -TargetPath "C:\Users\你的用户名\.trae-cn\skills\"
```

#### Python 版本（跨平台）：

从配置文件同步所有技能：

```bash
python python/sync-skills.py -ConfigFile "sync-config.json"
```

同步单个技能：

```bash
python python/sync-skills.py -SkillName "skill-name" -RepoPath "E:\path\to\repo" -TargetPath "C:\Users\你的用户名\.trae-cn\skills\"
```

同步仓库中的所有技能：

```bash
python python/sync-skills.py -RepoPath "E:\path\to\repo" -TargetPath "C:\Users\你的用户名\.trae-cn\skills\"
```

#### Java 版本（跨平台）：

从配置文件同步所有技能：

```bash
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.SyncSkills -ConfigFile "sync-config.json"
```

同步单个技能：

```bash
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.SyncSkills -SkillName "skill-name" -RepoPath "E:\path\to\repo" -TargetPath "C:\Users\你的用户名\.trae-cn\skills\"
```

同步仓库中的所有技能：

```bash
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.SyncSkills -RepoPath "E:\path\to\repo" -TargetPath "C:\Users\你的用户名\.trae-cn\skills\"
```

**初始化技能仓库：**

```bash
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.InitSkillRepo -RepoUrl "https://github.com/anthropics/skills.git" -LocalPath "E:\path\to\local\repo"
```

**从配置文件初始化多个仓库：**

```bash
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.InitSkillRepo -ConfigFile "init-config.json"
```

## 配置文件说明

### skills.csv

| 列名 | 说明 |
|------|------|
| repoPath | 包含技能的 Git 仓库路径 |
| skillName | 要同步的技能目录名称 |
| description | 技能的简要描述 |

### sync-config.json

```json
{
    "targetPath": "C:\Users\admin\.trae-cn\skills\",
    "repos": [
        {
            "repoPath": "E:\path\to\repo1",
            "skillNames": ["skill-name-1", "skill-name-2"]
        },
        {
            "repoPath": "E:\path\to\repo2",
            "skillNames": ["skill-name-3"]
        }
    ]
}
```

### batch-config.json

```json
{
  "batchOperations": [
    {
      "type": "init",
      "repos": [
        {
          "repoUrl": "https://github.com/anthropics/skills.git",
          "localPath": "E:\develop\code\open-source\github\skills\anthropics\skills\skills"
        },
        {
          "repoUrl": "https://github.com/staruhub/ClaudeSkills.git",
          "localPath": "E:\develop\code\open-source\github\skills\staruhub\ClaudeSkills\skills"
        }
      ]
    },
    {
      "type": "sync",
      "targetPath": "C:\Users\admin\.trae-cn\skills\",
      "repos": [
        {
          "repoPath": "E:\develop\code\open-source\github\skills\anthropics\skills\skills",
          "skillNames": ["algorithmic-art", "brand-guidelines"]
        },
        {
          "repoPath": "E:\develop\code\open-source\github\skills\staruhub\ClaudeSkills\skills",
          "skillNames": ["request-analyzer"]
        }
      ]
    }
  ]
}
```

### init-config.json

```json
{
  "repos": [
    {
      "repoUrl": "https://github.com/anthropics/skills.git",
      "localPath": "E:\develop\code\open-source\github\skills\anthropics\skills\skills"
    },
    {
      "repoUrl": "https://github.com/staruhub/ClaudeSkills.git",
      "localPath": "E:\develop\code\open-source\github\skills\staruhub\ClaudeSkills\skills"
    }
  ]
}
```

## 工作原理

### PowerShell 版本：
1. **csv-to-json.ps1**：
   - 读取 `skills.csv`，自动检测/转换 UTF-8 编码
   - 按 `repoPath` 合并技能（一对多关系）
   - 更新 `sync-config.json`（只增不改，不删除）

2. **sync-skills.ps1**：
   - 读取 `sync-config.json`
   - 对每个技能执行：`git pull` → `robocopy /E /MIR`
   - 复制前后打印目录树用于验证

### Python 版本：
1. **csv-to-json.py**：
   - 读取 `skills.csv`，自动检测/转换 UTF-8 编码
   - 按 `repoPath` 合并技能（一对多关系）
   - 更新 `sync-config.json`（只增不改，不删除）

2. **sync-skills.py**：
   - 读取 `sync-config.json`
   - 对每个技能执行：`git pull` → 跨平台文件复制
   - 复制前后打印目录树用于验证

### Java 版本：
1. **CsvToJson.java**：
   - 读取 `skills.csv`，自动检测/转换 UTF-8 编码
   - 按 `repoPath` 合并技能（一对多关系）
   - 更新 `sync-config.json`（只增不改，不删除）

2. **SyncSkills.java**：
   - 读取 `sync-config.json`
   - 对每个技能执行：`git pull` → 跨平台文件复制
   - 复制前后打印目录树用于验证

3. **InitSkillRepo.java**：
   - 克隆远程 Git 仓库到本地路径
   - 支持命令行参数指定仓库 URL 和本地路径
   - 提供详细的克隆过程日志

4. **BatchOperations.java**：
   - 读取 `batch-config.json` 配置文件
   - 顺序执行多个操作
   - 对于 `init` 操作：调用 `InitSkillRepo` 克隆仓库
   - 对于 `sync` 操作：创建临时配置并调用 `SyncSkills`
   - 提供所有操作的详细日志

## 系统要求

### PowerShell 版本（仅 Windows）：
- Windows PowerShell 5.1+
- Git（用于 `git pull`）
- Robocopy（Windows 内置）

### Python 版本（跨平台）：
- Python 3.6+
- Git（用于 `git pull`）

### Java 版本（跨平台）：
- Java 8+
- Maven 3.6+
- Git（用于 `git pull`）

## 注意事项

- CSV 文件如果使用非 UTF-8 编码会自动转换
- 相同 `repoPath` 的技能会合并到同一个 JSON 条目中
- `robocopy /MIR` 会镜像源目录，删除目标目录中的多余文件
- 运行 `csv-to-json.ps1` 前请关闭其他应用中的 CSV 文件，避免文件锁定

## 许可证

MIT License
