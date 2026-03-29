# Git 操作指南

## 一、已执行的 Git 操作总结

### 1. 初始化仓库
```powershell
git init
```
在 `E:\develop\code\skill-manager` 目录下初始化了 Git 仓库。

### 2. 配置用户信息
```powershell
git config user.email "lshgw14@users.noreply.github.com"
git config user.name "lshgw14"
```
配置了本地仓库的用户名和邮箱。

### 3. 添加文件到暂存区
```powershell
git add .
```
添加了以下文件（`.trae` 目录已被 `.gitignore` 排除）：
- `.gitignore`
- `README.md`
- `README_CN.md`
- `csv-to-json.ps1`
- `skills.csv`
- `sync-config.json`
- `sync-skills.ps1`

### 4. 提交代码
```powershell
git commit -m "Initial commit: Skill Manager tool with Chinese README"
```
提交记录：`[master 8bb89d9] Initial commit: Skill Manager tool with Chinese README`

### 5. 添加远程仓库
```powershell
git remote add github git@github.com:lshgw14/skill-manager.git
git remote add gitee git@gitee.com:lshgw14/skill-manager.git
```

### 6. 当前远程仓库配置
```
github  git@github.com:lshgw14/skill-manager.git (fetch)
github  git@github.com:lshgw14/skill-manager.git (push)
gitee   git@gitee.com:lshgw14/skill-manager.git (fetch)
gitee   git@gitee.com:lshgw14/skill-manager.git (push)
```

---

## 二、日常提交代码指南

### 提交到两个仓库

#### 方法一：分别推送
```powershell
# 查看修改状态
git status

# 添加修改的文件
git add .

# 提交
git commit -m "描述你的修改"

# 推送到 GitHub
git push github master

# 推送到 Gitee
git push gitee master
```

#### 方法二：同时推送到两个仓库
```powershell
# 一次性推送到所有远程仓库
git push --all
```

#### 方法三：设置默认推送
```powershell
# 设置 GitHub 为默认上游
git branch --set-upstream-to=github/master master

# 之后只需 git push 即可推送到 GitHub
git push

# 推送到 Gitee
git push gitee master
```

---

## 三、拉取更新指南

### 从远程仓库拉取

#### 从 GitHub 拉取
```powershell
git pull github master
```

#### 从 Gitee 拉取
```powershell
git pull gitee master
```

### 同步两个仓库

如果在一个仓库有更新，同步到另一个仓库：
```powershell
# 从 GitHub 拉取最新代码
git pull github master

# 推送到 Gitee
git push gitee master
```

---

## 四、冲突解决指南

### 冲突场景

当本地和远程都有修改时，可能会产生冲突：

```
CONFLICT (content): Merge conflict in filename
Automatic merge failed; fix conflicts and then commit the result.
```

### 解决步骤

#### 1. 查看冲突文件
```powershell
git status
```
冲突的文件会显示 `both modified`。

#### 2. 打开冲突文件
冲突内容格式：
```
<<<<<<< HEAD
本地修改的内容
=======
远程修改的内容
>>>>>>> github/master
```

#### 3. 手动解决冲突
删除冲突标记，保留需要的内容：
```
最终保留的内容
```

#### 4. 标记冲突已解决
```powershell
git add <冲突文件>
```

#### 5. 完成合并
```powershell
git commit -m "Merge: resolve conflicts"
```

#### 6. 推送到远程
```powershell
git push github master
git push gitee master
```

### 放弃合并
如果想放弃当前合并操作：
```powershell
git merge --abort
```

### 使用工具解决冲突
推荐使用 VS Code 的内置合并工具：
1. 打开冲突文件
2. 点击 "Accept Current Change" / "Accept Incoming Change" / "Accept Both Changes"

---

## 五、常见问题处理

### 1. 推送失败：远程仓库不存在

**错误信息**：
```
fatal: repository 'git@github.com:lshgw14/skill-manager.git/' not found
```

**解决方案**：
- 先在 GitHub/Gitee 网站上创建仓库 `skill-manager`
- 仓库创建后再执行 `git push`

### 2. 推送失败：权限问题

**错误信息**：
```
git@github.com: Permission denied (publickey)
```

**解决方案**：
- 确保 SSH 密钥已添加到 GitHub/Gitee 账户
- 检查 SSH 密钥配置：
  ```powershell
  # 测试 SSH 连接
  ssh -T git@github.com
  ssh -T git@gitee.com
  ```

### 3. 推送失败：历史不一致

**错误信息**：
```
! [rejected]        master -> master (fetch first)
```

**解决方案**：
```powershell
# 先拉取远程代码
git pull github master --rebase

# 再推送
git push github master
```

### 4. 强制推送（谨慎使用）

```powershell
# 强制覆盖远程仓库（会丢失远程历史）
git push github master --force
```

⚠️ **警告**：强制推送会覆盖远程仓库的历史，仅在确定需要时使用。

### 5. 查看提交历史
```powershell
# 查看简洁历史
git log --oneline

# 查看详细历史
git log

# 查看图形历史
git log --oneline --graph --all
```

### 6. 撤销最近一次提交（未推送）
```powershell
# 保留修改
git reset --soft HEAD~1

# 丢弃修改
git reset --hard HEAD~1
```

### 7. 修改最后一次提交信息
```powershell
git commit --amend -m "新的提交信息"
```

---

## 六、常用命令速查

| 操作 | 命令 |
|------|------|
| 查看状态 | `git status` |
| 添加文件 | `git add .` |
| 提交 | `git commit -m "message"` |
| 推送到 GitHub | `git push github master` |
| 推送到 Gitee | `git push gitee master` |
| 从 GitHub 拉取 | `git pull github master` |
| 从 Gitee 拉取 | `git pull gitee master` |
| 查看远程仓库 | `git remote -v` |
| 查看提交历史 | `git log --oneline` |
| 查看分支 | `git branch -a` |

---

## 七、工作流建议

### 推荐的日常工作流

```
1. 开始工作前
   git pull github master

2. 修改代码
   # 编辑文件...

3. 提交修改
   git add .
   git commit -m "描述修改内容"

4. 推送到两个仓库
   git push github master
   git push gitee master
```

### 功能开发工作流

```powershell
# 创建新分支
git checkout -b feature/new-feature

# 开发完成后合并
git checkout master
git merge feature/new-feature

# 推送
git push github master
git push gitee master

# 删除功能分支
git branch -d feature/new-feature
```
