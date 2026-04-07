# Skill Manager

A cross-platform skill synchronization tool for managing and syncing skills from multiple Git repositories to a target directory, available in both PowerShell and Python versions.

## Features

- **Multi-repo Support**: Sync skills from multiple Git repositories
- **Config-driven**: Use JSON config file for batch synchronization
- **CSV Management**: Easy skill management via CSV file with auto-conversion to JSON
- **Git Integration**: Automatic `git pull` before each sync
- **Robocopy Powered**: Reliable file copying with mirror mode
- **Encoding Auto-detection**: Automatic UTF-8 conversion for CSV files
- **Detailed Logging**: Comprehensive logging with directory tree verification
- **Batch Operations**: Execute multiple operations (init and sync) in sequence via configuration file

## File Structure

```
skill-manager/
├── powershell/          # PowerShell version (Windows only)
│   ├── sync-skills.ps1  # Main sync script
│   └── csv-to-json.ps1  # CSV to JSON config converter
├── python/              # Python version (cross-platform)
│   ├── sync-skills.py   # Main sync script
│   └── csv-to-json.py   # CSV to JSON config converter
├── java/                # Java version (cross-platform)
│   ├── src/main/java/com/skillmanager/
│   │   ├── CsvToJson.java       # CSV to JSON config converter
│   │   ├── SyncSkills.java       # Main sync script
│   │   ├── InitSkillRepo.java    # Repo initialization script
│   │   └── BatchOperations.java  # Batch operations script
│   ├── pom.xml          # Maven configuration
│   ├── build.bat         # Windows build script
│   └── build.sh         # Linux/macOS build script
├── skills.csv           # Skill list (CSV format)
├── sync-config.json     # Sync configuration (JSON format)
├── batch-config.json    # Batch operations configuration (JSON format)
├── README.md            # This file
└── README_CN.md         # Chinese README
```

## Usage

### 1. Prepare CSV Configuration

Edit `skills.csv` to add your skills:

```csv
repoPath,skillName,description
E:\path\to\repo1,skill-name-1,Skill description 1
E:\path\to\repo1,skill-name-2,Skill description 2
E:\path\to\repo2,skill-name-3,Skill description 3
```

### 2. Convert CSV to JSON

#### PowerShell Version (Windows only):

```powershell
.\powershell\csv-to-json.ps1
```

Or with custom parameters:

```powershell
.\powershell\csv-to-json.ps1 -CsvFile ".\skills.csv" -JsonFile ".\sync-config.json" -TargetPath "C:\Users\your-username\.trae-cn\skills\"
```

#### Python Version (cross-platform):

```bash
python python/csv-to-json.py
```

Or with custom parameters:

```bash
python python/csv-to-json.py -CsvFile "skills.csv" -JsonFile "sync-config.json" -TargetPath "C:\Users\your-username\.trae-cn\skills\"
```

#### Java Version (cross-platform):

First, build the project:

```bash
# Windows
.\java\build.bat

# Linux/macOS
chmod +x java/build.sh
./java/build.sh
```

Then run the scripts:

**Convert CSV to JSON:**

```bash
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.CsvToJson
```

Or with custom parameters:

```bash
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.CsvToJson -CsvFile "skills.csv" -JsonFile "sync-config.json" -TargetPath "C:\Users\your-username\.trae-cn\skills\"
```

### 3. Sync Skills

#### PowerShell Version (Windows only):

Sync all skills from config file:

```powershell
.\powershell\sync-skills.ps1 -ConfigFile ".\sync-config.json"
```

Sync a single skill:

```powershell
.\powershell\sync-skills.ps1 -SkillName "skill-name" -RepoPath "E:\path\to\repo" -TargetPath "C:\Users\your-username\.trae-cn\skills\"
```

Sync all skills from a repository:

```powershell
.\powershell\sync-skills.ps1 -RepoPath "E:\path\to\repo" -TargetPath "C:\Users\your-username\.trae-cn\skills\"
```

#### Python Version (cross-platform):

Sync all skills from config file:

```bash
python python/sync-skills.py -ConfigFile "sync-config.json"
```

Sync a single skill:

```bash
python python/sync-skills.py -SkillName "skill-name" -RepoPath "E:\path\to\repo" -TargetPath "C:\Users\your-username\.trae-cn\skills\"
```

Sync all skills from a repository:

```bash
python python/sync-skills.py -RepoPath "E:\path\to\repo" -TargetPath "C:\Users\your-username\.trae-cn\skills\"
```

#### Java Version (cross-platform):

**Sync all skills from config file:**

```bash
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.SyncSkills -ConfigFile "sync-config.json"
```

**Sync a single skill:**

```bash
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.SyncSkills -SkillName "skill-name" -RepoPath "E:\path\to\repo" -TargetPath "C:\Users\your-username\.trae-cn\skills\"
```

**Sync all skills from a repository:**

```bash
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.SyncSkills -RepoPath "E:\path\to\repo" -TargetPath "C:\Users\your-username\.trae-cn\skills\"
```

**Initialize a skill repository:**

```bash
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.InitSkillRepo -RepoUrl "https://github.com/anthropics/skills.git" -LocalPath "E:\path\to\local\repo"
```

**Execute batch operations:**

```bash
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.BatchOperations
```

Or with custom configuration file:

```bash
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.BatchOperations -ConfigFile "batch-config.json"
```

## Configuration Files

### skills.csv

| Column | Description |
|--------|-------------|
| repoPath | Path to the Git repository containing skills |
| skillName | Name of the skill directory to sync |
| description | Brief description of the skill |

### sync-config.json

```json
{
    "targetPath": "C:\\Users\admin\\.trae-cn\\skills\\",
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

### batch-config.json

```json
{
  "batchOperations": [
    {
      "type": "init",
      "repos": [
        {
          "repoUrl": "https://github.com/anthropics/skills.git",
          "localPath": "E:\\develop\\code\\open-source\\github\\skills\\anthropics\\skills\\skills"
        },
        {
          "repoUrl": "https://github.com/staruhub/ClaudeSkills.git",
          "localPath": "E:\\develop\\code\\open-source\\github\\skills\\staruhub\\ClaudeSkills\\skills"
        }
      ]
    },
    {
      "type": "sync",
      "targetPath": "C:\\Users\\admin\\.trae-cn\\skills\\",
      "repos": [
        {
          "repoPath": "E:\\develop\\code\\open-source\\github\\skills\\anthropics\\skills\\skills",
          "skillNames": ["algorithmic-art", "brand-guidelines"]
        },
        {
          "repoPath": "E:\\develop\\code\\open-source\\github\\skills\\staruhub\\ClaudeSkills\\skills",
          "skillNames": ["request-analyzer"]
        }
      ]
    }
  ]
}
```

## How It Works

### PowerShell Version:
1. **csv-to-json.ps1**:
   - Reads `skills.csv` with auto UTF-8 encoding detection/conversion
   - Merges skills by `repoPath` (one-to-many relationship)
   - Updates `sync-config.json` (add/modify only, no deletion)

2. **sync-skills.ps1**:
   - Reads `sync-config.json`
   - For each skill: `git pull` → `robocopy /E /MIR`
   - Logs directory tree before and after copy for verification

### Python Version:
1. **csv-to-json.py**:
   - Reads `skills.csv` with auto UTF-8 encoding detection/conversion
   - Merges skills by `repoPath` (one-to-many relationship)
   - Updates `sync-config.json` (add/modify only, no deletion)

2. **sync-skills.py**:
   - Reads `sync-config.json`
   - For each skill: `git pull` → cross-platform file copy
   - Logs directory tree before and after copy for verification

### Java Version:
1. **CsvToJson.java**:
   - Reads `skills.csv` with auto UTF-8 encoding detection/conversion
   - Merges skills by `repoPath` (one-to-many relationship)
   - Updates `sync-config.json` (add/modify only, no deletion)

2. **SyncSkills.java**:
   - Reads `sync-config.json`
   - For each skill: `git pull` → cross-platform file copy
   - Logs directory tree before and after copy for verification

3. **InitSkillRepo.java**:
   - Clones a remote Git repository to a local path
   - Supports command-line parameters for repo URL and local path
   - Provides detailed logging of the cloning process

4. **BatchOperations.java**:
   - Reads `batch-config.json` configuration file
   - Executes multiple operations in sequence
   - For `init` operations: calls `InitSkillRepo` to clone repositories
   - For `sync` operations: creates temporary config and calls `SyncSkills`
   - Provides comprehensive logging of all operations

## Requirements

### PowerShell Version (Windows only):
- Windows PowerShell 5.1+
- Git (for `git pull`)
- Robocopy (built-in on Windows)

### Python Version (cross-platform):
- Python 3.6+
- Git (for `git pull`)

### Java Version (cross-platform):
- Java 8+
- Maven 3.6+
- Git (for `git pull`)

## Notes

- The CSV file will be automatically converted to UTF-8 if it uses a different encoding
- Skills with the same `repoPath` will be merged into a single entry in JSON
- `robocopy /MIR` will mirror the source directory, deleting extra files in target
- Close CSV file in other applications before running `csv-to-json.ps1` to avoid file lock issues

## License

MIT License
