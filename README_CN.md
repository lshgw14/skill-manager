# Skill Manager 技能同步工具

一个基于 PowerShell 的技能同步工具，用于从多个 Git 仓库同步技能文件到目标目录。

## 功能特性

- **多仓库支持**：从多个 Git 仓库同步技能
- **配置驱动**：使用 JSON 配置文件进行批量同步
- **CSV 管理**：通过 CSV 文件轻松管理技能，自动转换为 JSON
- **Git 集成**：每次同步前自动执行 `git pull`
- **Robocopy 驱动**：使用镜像模式进行可靠的文件复制
- **编码自动检测**：自动将 CSV 文件转换为 UTF-8 编码
- **详细日志**：完整的日志记录，包含目录树验证

## 文件结构

```
skill-manager/
├── sync-skills.ps1      # 主同步脚本
├── csv-to-json.ps1      # CSV 转 JSON 配置工具
├── skills.csv           # 技能列表（CSV 格式）
├── sync-config.json     # 同步配置（JSON 格式）
└── README.md            # 说明文档
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

```powershell
.\csv-to-json.ps1
```

或使用自定义参数：

```powershell
.\csv-to-json.ps1 -CsvFile ".\skills.csv" -JsonFile ".\sync-config.json" -TargetPath "C:\Users\你的用户名\.trae-cn\skills\"
```

### 3. 同步技能

从配置文件同步所有技能：

```powershell
.\sync-skills.ps1 -ConfigFile ".\sync-config.json"
```

同步单个技能：

```powershell
.\sync-skills.ps1 -SkillName "skill-name" -RepoPath "E:\path\to\repo" -TargetPath "C:\Users\你的用户名\.trae-cn\skills\"
```

同步仓库中的所有技能：

```powershell
.\sync-skills.ps1 -RepoPath "E:\path\to\repo" -TargetPath "C:\Users\你的用户名\.trae-cn\skills\"
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
    "targetPath": "C:\\Users\\admin\\.trae-cn\\skills\\",
    "repos": [
        {
            "repoPath": "E:\\path\\to\\repo1",
            "skillNames": ["skill-name-1", "skill-name-2"]
        },
        {
            "repoPath": "E:\\path\\to\\repo2",
            "skillNames": ["skill-name-3"]
        }
    ]
}
```

## 工作原理

1. **csv-to-json.ps1**：
   - 读取 `skills.csv`，自动检测/转换 UTF-8 编码
   - 按 `repoPath` 合并技能（一对多关系）
   - 更新 `sync-config.json`（只增不改，不删除）

2. **sync-skills.ps1**：
   - 读取 `sync-config.json`
   - 对每个技能执行：`git pull` → `robocopy /E /MIR`
   - 复制前后打印目录树用于验证

## 系统要求

- Windows PowerShell 5.1+
- Git（用于 `git pull`）
- Robocopy（Windows 内置）

## 注意事项

- CSV 文件如果使用非 UTF-8 编码会自动转换
- 相同 `repoPath` 的技能会合并到同一个 JSON 条目中
- `robocopy /MIR` 会镜像源目录，删除目标目录中的多余文件
- 运行 `csv-to-json.ps1` 前请关闭其他应用中的 CSV 文件，避免文件锁定

## 许可证

MIT License
