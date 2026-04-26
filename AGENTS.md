# AGENTS.md - Skill Manager

Cross-platform skill sync tool: syncs skill directories from Git repos to a target directory.

## Pipeline

```
skills.csv  --csv-to-json-->  sync-config.json  --sync-skills-->  target dir
```

1. Edit `skills.csv` (columns: `repoPath,repoName,localPath,repoUrl,skillName,description`)
2. Run `csv-to-json.py` (or .ps1 / Java CsvToJson) -- merges skills by `repoPath`, add/modify only (no deletion)
3. Run `sync-skills.py -ConfigFile sync-config.json` (or .ps1 / Java SyncSkills) -- `git pull` then copy

## Implementations

| Language | File | Dependencies |
|----------|------|-------------|
| PowerShell (Win only) | `powershell/` | git CLI + robocopy (`/E /MIR` mirrors, deletes extras) |
| **Python** (recommended) | `python/` | Python 3.6+ + git CLI |
| Java | `java/` | Java 21, Maven. Uses **JGIT** (no external git CLI) |
| Go | `go/` | `init-git-ssh` only (SSH keygen + SSH config) |

## Java

```bash
.\java\build.bat                               # mvn clean package → fat JAR
java -cp java/target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.<Class> [args]
```

Runnable classes (all use `-ConfigFile`, `-SkillName`, `-RepoPath`, `-TargetPath` etc. with PascalCase flags):
- `CsvToJson`     (default mainClass in pom.xml) + `-OutputType init` for init-config.json
- `SyncSkills`
- `InitSkillRepo` + `-RepoUrl` / `-LocalPath`
- `InitGitSsh`    + `-Email`, `-Action`, `-Server`, `-ConfigFile`, `-GitConfig`
- `BatchOperations` reads `batch-config.json` (chains init + sync + ssh)

## Go

```bash
cd go && go build -o bin/<name>.exe <file>.go   # or .\build.bat
go/bin/ has pre-built exes for: sync-skills, csv-to-json, init-skill-repo, batch-operations, init-git-ssh
```

## csv-to-json behavior

- Reads CSV, auto-detects encoding, converts to UTF-8
- **Add/modify only**: existing skills in JSON are preserved; removed CSV rows are NOT deleted from JSON
- Merges skills with same `repoPath` into one entry under `skillNames`
- With `-OutputType init`: generates `init-config.json` (for cloning repos) instead of `sync-config.json`

## sync-config.json format

```json
{ "targetPath": "C:\\path\\to\\target\\", "repos": [
  { "repoPath": "E:\\repo\\skills", "repoName": "alias", "skillNames": ["skill1", "skill2"] }
]}
```

## Gotchas

- All implementations use **`-PascalCase` flags** (e.g. `-ConfigFile`, `-SkillName`), not `--kebab-case`
- Python/PowerShell/Go require **git CLI** on PATH; Java uses JGIT (embedded)
- `sync-config.json` has a flat structure (single `targetPath` + `repos` array)
- `batch-config.json` chains operation types: `"type": "init"`, `"type": "sync"`, `"type": "ssh"`
- `.gitignore` excludes: `.trae/`, `target/`, `.vscode/`, `.idea/`
- Git remotes: `github` and `gitee` (per `GIT_GUIDE.md`)
- No tests, no CI
