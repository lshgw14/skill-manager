# Skill Manager 技能同步工具

一个跨平台的技能同步工具，用于从多个 Git 仓库同步技能文件到目标目录，同时提供 PowerShell 和 Python 版本。

## 使用方法

### Python 版本（跨平台）：

#### 1. 转换 CSV 为 JSON

```bash
python python/csv-to-json.py
```

或使用自定义参数：

```bash
python python/csv-to-json.py -CsvFile "skills.csv" -JsonFile "sync-config.json" -TargetPath "C:\Users\你的用户名\.trae-cn\skills\"
```

#### 2. 同步技能

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

### PowerShell 版本（仅 Windows）：

#### 1. 转换 CSV 为 JSON

```powershell
.\powershell\csv-to-json.ps1
```

或使用自定义参数：

```powershell
.\powershell\csv-to-json.ps1 -CsvFile ".\skills.csv" -JsonFile ".\sync-config.json" -TargetPath "C:\Users\你的用户名\.trae-cn\skills\"
```

#### 2. 同步技能

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

## 系统要求

### Python 版本（跨平台）：
- Python 3.6+
- Git（用于 `git pull`）

### PowerShell 版本（仅 Windows）：
- Windows PowerShell 5.1+
- Git（用于 `git pull`）
- Robocopy（Windows 内置）