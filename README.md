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
- **SSH Support**: Clone repositories using SSH URLs (Java version)

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
repoPath,repoName,localPath,repoUrl,skillName,description
E:\path\to\repo1,anthropics_skills,E:\path\to\repo1,https://github.com/anthropics/skills.git,skill-name-1,Skill description 1
E:\path\to\repo1,anthropics_skills,E:\path\to\repo1,https://github.com/anthropics/skills.git,skill-name-2,Skill description 2
E:\path\to\repo2,staruhub_ClaudeSkills,E:\path\to\repo2,https://github.com/staruhub/ClaudeSkills.git,skill-name-3,Skill description 3
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

The Java version uses Eclipse JGIT library for Git operations, which provides better cross-platform compatibility and eliminates the need for Git command line tool.

**Advantages of using JGIT:**
- No dependency on Git command line tool
- Better cross-platform compatibility
- More flexible error handling
- Cleaner code structure

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

### 4. Initialize Skill Repositories

#### Java Version (cross-platform):

**Initialize a skill repository (HTTPS):**

```bash
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.InitSkillRepo -RepoUrl "https://github.com/anthropics/skills.git" -LocalPath "E:\path\to\local\repo"
```

**Initialize a skill repository (SSH):**

```bash
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.InitSkillRepo -RepoUrl "git@github.com:anthropics/skills.git" -LocalPath "E:\path\to\local\repo"
```

**Initialize multiple repositories from config file:**

```bash
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.InitSkillRepo -ConfigFile "init-config.json"
```

**Execute batch operations:**

```bash
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.BatchOperations
```

Or with custom configuration file:

```bash
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.BatchOperations -ConfigFile "batch-config.json"
```

### 5. Initialize Git SSH Configuration

#### Java Version (cross-platform):

**Generate SSH keys, configure SSH config, and set Git settings for all Git servers:**

```bash
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.InitGitSsh -Email "your-email@example.com"
```

**Generate SSH keys only for specific Git server:**

```bash
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.InitGitSsh -Email "your-email@example.com" -Action "generate" -Server "github"
```

**Configure SSH config only:**

```bash
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.InitGitSsh -Email "your-email@example.com" -Action "config" -Server "all"
```

**Using YML configuration file:**

```bash
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.InitGitSsh -ConfigFile "git-ssh-config.yml"
```

**Using YML configuration file without Git config:**

```bash
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.InitGitSsh -ConfigFile "git-ssh-config.yml" -GitConfig false
```

## Configuration Files

### skills.csv

| Column | Description |
|--------|-------------|
| repoPath | Path to the Git repository containing skills |
| repoName | A friendly name for the repository (e.g., "anthropics_skills" for "E:\develop\code\open-source\github\skills\anthropics\skills\skills") |
| localPath | Local path to the Git repository (used for init-config.json) |
| repoUrl | GitHub repository URL (e.g., "https://github.com/anthropics/skills.git") |
| skillName | Name of the skill directory to sync |
| description | Brief description of the skill |

### sync-config.json

```json
{
    "targetPath": "C:\\Users\\admin\\.trae-cn\\skills\\",
    "repos": [
        {
            "repoName": "anthropics_skills",
            "repoPath": "E:\\path\\to\\repo1",
            "skillNames": ["skill-name-1", "skill-name-2"]
        },
        {
            "repoName": "staruhub_ClaudeSkills",
            "repoPath": "E:\\path\\to\\repo2",
            "skillNames": ["skill-name-3"]
        }
    ]
}
```

**Fields explanation:**
- `repoName`: A friendly name for the repository (e.g., "anthropics_skills" for "E:\develop\code\open-source\github\skills\anthropics\skills\skills")
- `repoPath`: The local path to the repository
- `skillNames`: List of skill names to sync

### batch-config.json

```json
{
  "batchOperations": [
    {
      "type": "init",
      "repos": [
        {
          "repoName": "anthropics_skills",
          "repoUrl": "https://github.com/anthropics/skills.git",
          "localPath": "E:\\develop\\code\\open-source\\github\\skills\\anthropics"
        },
        {
          "repoName": "staruhub_ClaudeSkills",
          "repoUrl": "https://github.com/staruhub/ClaudeSkills.git",
          "localPath": "E:\\develop\\code\\open-source\\github\\skills\\staruhub"
        },
        {
          "repoName": "obra_superpowers",
          "repoUrl": "https://github.com/obra/superpowers.git",
          "localPath": "E:\\develop\\code\\open-source\\github\\skills\\obra"
        }
      ]
    },
    {
      "type": "sync",
      "targetPath": "C:\\Users\\admin\\.trae-cn\\skills\\",
      "repos": [
        {
          "repoName": "anthropics_skills",
          "repoPath": "E:\\develop\\code\\open-source\\github\\skills\\anthropics\\skills\\skills",
          "skillNames": ["algorithmic-art", "brand-guidelines"]
        },
        {
          "repoName": "staruhub_ClaudeSkills",
          "repoPath": "E:\\develop\\code\\open-source\\github\\skills\\staruhub\\ClaudeSkills\\skills",
          "skillNames": ["request-analyzer"]
        },
        {
          "repoName": "obra_superpowers",
          "repoPath": "E:\\develop\\code\\open-source\\github\\skills\\obra\\superpowers\\skills",
          "skillNames": ["brainstorming", "writing-plans"]
        }
      ]
    }
  ]
}
```

**Fields explanation for init operation:**
- `repoName`: A friendly name for the repository (e.g., "anthropics_skills" for "https://github.com/anthropics/skills.git")
- `repoUrl`: The Git repository URL
- `localPath`: The local path to clone the repository to

**Fields explanation for sync operation:**
- `repoName`: A friendly name for the repository
- `repoPath`: The local path to the repository
- `skillNames`: List of skill names to sync

### init-config.json

```json
{
  "repos": [
    {
      "repoName": "anthropics_skills",
      "repoUrl": "https://github.com/anthropics/skills.git",
      "localPath": "E:\\develop\\code\\open-source\\github\\skills\\anthropics"
    },
    {
      "repoName": "staruhub_ClaudeSkills",
      "repoUrl": "git@github.com:staruhub/ClaudeSkills.git",
      "localPath": "E:\\develop\\code\\open-source\\github\\skills\\staruhub"
    }
  ]
}
```

**Fields explanation:**
- `repoName`: A friendly name for the repository (e.g., "anthropics_skills" for "https://github.com/anthropics/skills.git")
- `repoUrl`: The Git repository URL
- `localPath`: The local path to clone the repository to

## How It Works

### PowerShell Version:
1. **csv-to-json.ps1**:
   - Reads `skills.csv` with auto UTF-8 encoding detection/conversion
   - Merges skills by `repoPath` (one-to-many relationship)
   - Updates `sync-config.json` (add/modify only, no deletion)
   - Supports conversion to `init-config.json` with `-OutputType "init"` parameter

2. **sync-skills.ps1**:
   - Reads `sync-config.json`
   - For each skill: `git pull` → `robocopy /E /MIR`
   - Logs directory tree before and after copy for verification

### Python Version:
1. **csv-to-json.py**:
   - Reads `skills.csv` with auto UTF-8 encoding detection/conversion
   - Merges skills by `repoPath` (one-to-many relationship)
   - Updates `sync-config.json` (add/modify only, no deletion)
   - Supports conversion to `init-config.json` with `-OutputType "init"` parameter

2. **sync-skills.py**:
   - Reads `sync-config.json`
   - For each skill: `git pull` → cross-platform file copy
   - Logs directory tree before and after copy for verification

### Java Version:
1. **CsvToJson.java**:
   - Reads `skills.csv` with auto UTF-8 encoding detection/conversion
   - Merges skills by `repoPath` (one-to-many relationship)
   - Updates `sync-config.json` (add/modify only, no deletion)
   - Supports conversion to `init-config.json` with `-OutputType "init"` parameter

2. **SyncSkills.java**:
   - Reads `sync-config.json`
   - For each skill: `git pull` → cross-platform file copy
   - Logs directory tree before and after copy for verification

3. **InitSkillRepo.java**:
   - Clones a remote Git repository to a local path
   - Supports command-line parameters for repo URL and local path
   - Provides detailed logging of the cloning process
   - Supports both HTTPS and SSH URLs for cloning

4. **BatchOperations.java**:
   - Reads `batch-config.json` configuration file
   - Executes multiple operations in sequence
   - For `init` operations: calls `InitSkillRepo` to clone repositories
   - For `sync` operations: creates temporary config and calls `SyncSkills`
   - For `ssh` operations: calls `InitGitSsh` to configure SSH settings
   - Provides comprehensive logging of all operations

5. **InitGitSsh.java**:
   - Generates SSH key pairs for Git servers
   - Configures SSH config file with server-specific settings
   - Supports multiple Git servers (Gitee, GitHub, GitCode)
   - Provides command-line interface for configuration
   - Supports configuration via YML and JSON files
   - Configures global Git settings (user.name, user.email, etc.)
   - Configures repository-specific Git settings
   - Supports control of Git configuration via -GitConfig parameter

## Requirements

### PowerShell Version (Windows only):
- Windows PowerShell 5.1+
- Git (for `git pull`)
- Robocopy (built-in on Windows)

### Python Version (cross-platform):
- Python 3.6+
- Git (for `git pull`)

### Java Version (cross-platform):
- Java 21+
- Maven 3.6+
- Git (for `git pull`)
- SSH support: Uses Apache MINA SSHD for SSH connections

## Notes

- The CSV file will be automatically converted to UTF-8 if it uses a different encoding
- Skills with the same `repoPath` will be merged into a single entry in JSON
- `robocopy /MIR` will mirror the source directory, deleting extra files in target
- Close CSV file in other applications before running `csv-to-json.ps1` to avoid file lock issues

## License

MIT License
