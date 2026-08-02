# 用户激励平台 Git 首次上传指南

本文以 Windows PowerShell、项目目录 `D:\Practise` 和默认分支 `main` 为例，重点说明如何把当前本地项目第一次上传到 GitHub、Gitee、GitLab 等远程 Git 仓库。

## 1. 当前项目状态

当前目录尚未执行 `git init`，因此还不是 Git 仓库。本机已经安装 Git，并配置了全局提交身份：

```text
user.name  = guox16
user.email = 1321000576@qq.com
```

提交身份会公开显示在提交记录中。如果不希望公开 QQ 邮箱，应在第一次提交前换成代码托管平台提供的隐私邮箱。

当前 `.gitignore` 已排除以下内容：

- `.env`：本地密码和环境变量。
- `target/`：Maven 编译产物。
- `node_modules/`、`web/dist/`：前端依赖和构建产物。
- `.idea/`、`.vscode/`、日志文件。

`.env.example` 会正常上传，因为它只保存示例配置。

## 2. 在远程平台创建空仓库

先登录 GitHub、Gitee 或 GitLab，创建一个新仓库，例如：

```text
incentive-platform
```

首次上传本地已有项目时，建议创建**完全空的远程仓库**：

- 不自动创建 README。
- 不自动创建 `.gitignore`。
- 不自动添加 License。

这样远程不会提前产生提交，可以避免第一次推送时出现历史冲突。

创建完成后，平台会提供远程地址。常见格式：

```text
# HTTPS
https://github.com/你的用户名/incentive-platform.git
https://gitee.com/你的用户名/incentive-platform.git

# SSH
git@github.com:你的用户名/incentive-platform.git
git@gitee.com:你的用户名/incentive-platform.git
```

后续命令中的 `<远程仓库地址>` 要替换为真实地址，不要保留尖括号。

## 3. 第一次初始化本地仓库

进入项目目录：

```powershell
Set-Location D:\Practise
```

确认当前路径，避免在错误目录初始化仓库：

```powershell
Get-Location
Get-ChildItem -Force
```

初始化 Git，并将默认分支统一命名为 `main`：

```powershell
git init
git branch -M main
```

`git init` 只会在本地创建 `.git` 元数据目录，不会上传文件，也不会修改业务代码。

## 4. 第一次提交前检查

先检查文件状态：

```powershell
git status
```

确认敏感配置确实被忽略：

```powershell
git check-ignore -v .env
git status --ignored
```

`git check-ignore` 应显示 `.gitignore` 中的 `.env` 规则。如果 `.env` 没有被忽略，先停止操作并修正 `.gitignore`。

将文件加入暂存区：

```powershell
git add .
```

此时仍未创建提交。必须再次检查暂存区：

```powershell
git status
git diff --cached --stat
git diff --cached
```

重点确认：

- 暂存区中没有 `.env`、真实密码、访问令牌或私钥。
- 没有 `target/`、`node_modules/` 等生成内容。
- `.env.example` 中只有 `change-me` 等示例值。
- 文件数量和修改内容符合预期。

如果误加入某个文件，可在首次提交前移出暂存区，文件本身不会被删除。由于此时还没有首个提交，使用 `git rm --cached`：

```powershell
git rm --cached -- 文件路径
```

目录需要增加 `-r`，例如 `git rm -r --cached -- target`。

## 5. 创建第一次提交

确认暂存区正确后执行：

```powershell
git commit -m "chore: initialize incentive platform"
```

检查提交是否创建成功：

```powershell
git log --oneline --decorate -5
git status
```

正常情况下，`git status` 会显示工作区干净，`git log` 会显示刚创建的初始化提交。

如果 Git 提示缺少用户名或邮箱，可仅为当前仓库配置：

```powershell
git config user.name "你的名字"
git config user.email "你的邮箱"
```

不加 `--global` 时，只影响当前项目。

## 6. 绑定远程仓库并首次推送

添加远程地址：

```powershell
git remote add origin <远程仓库地址>
```

检查地址，防止推送到错误仓库：

```powershell
git remote -v
```

首次推送：

```powershell
git push -u origin main
```

参数含义：

- `origin`：远程仓库的本地别名。
- `main`：要推送的本地分支。
- `-u`：建立本地 `main` 与远程 `origin/main` 的跟踪关系。

首次成功后，日常推送只需要：

```powershell
git push
```

最后到远程平台刷新页面，检查 README、`services`、`web`、`docs` 等内容是否完整，并确认 `.env` 没有出现。

## 7. HTTPS 和 SSH 如何选择

### HTTPS

优点是配置简单，适合第一次使用。GitHub 等平台通常不再接受账户密码直接推送，需要使用浏览器登录、凭据管理器或 Personal Access Token。

```powershell
git remote add origin https://github.com/你的用户名/incentive-platform.git
git push -u origin main
```

不要把 Token 写入远程 URL、脚本、README 或 `.env.example`。

### SSH

适合长期开发，配置一次后推送更方便。先检查是否已有公钥：

```powershell
Get-ChildItem $env:USERPROFILE\.ssh -ErrorAction SilentlyContinue
```

没有密钥时可生成 Ed25519 密钥：

```powershell
ssh-keygen -t ed25519 -C "你的邮箱"
```

复制公钥内容并添加到 GitHub/Gitee/GitLab 的 SSH Keys：

```powershell
Get-Content $env:USERPROFILE\.ssh\id_ed25519.pub
```

只能上传 `.pub` 公钥，绝不能上传或发送 `id_ed25519` 私钥。

测试连接：

```powershell
ssh -T git@github.com
```

然后使用 SSH 地址绑定远程：

```powershell
git remote add origin git@github.com:你的用户名/incentive-platform.git
git push -u origin main
```

## 8. 完整首次上传命令清单

远程仓库为空、`.gitignore` 已确认正确时，可以按下面顺序执行：

```powershell
Set-Location D:\Practise

git init
git branch -M main

git status
git check-ignore -v .env

git add .
git status
git diff --cached --stat

git commit -m "chore: initialize incentive platform"

git remote add origin <远程仓库地址>
git remote -v

git push -u origin main
```

不要省略两次 `git status` 和远程地址检查。

## 9. 远程仓库不是空仓库时

如果创建远程仓库时勾选了 README、License 或 `.gitignore`，远程已经有独立提交。最简单安全的做法是删除该远程仓库并重新创建一个空仓库，再执行第 8 节流程。

如果必须保留远程提交，先完成本地首次提交，再执行：

```powershell
git fetch origin
git log --oneline --graph --decorate --all
git merge origin/main --allow-unrelated-histories
```

如有冲突，逐个解决并提交合并结果，然后：

```powershell
git push -u origin main
```

不要为了绕过 `non-fast-forward` 错误直接使用 `git push --force`，否则可能覆盖远程已有内容。

## 10. 常见错误处理

### `src refspec main does not match any`

原因通常是还没有创建第一次提交。执行：

```powershell
git status
git commit -m "chore: initialize incentive platform"
git push -u origin main
```

### `remote origin already exists`

先查看现有地址：

```powershell
git remote -v
```

地址错误时修改，不需要重复添加：

```powershell
git remote set-url origin <正确的远程仓库地址>
```

### `rejected non-fast-forward`

说明远程包含本地没有的提交。先查看远程历史：

```powershell
git fetch origin
git log --oneline --graph --decorate --all
```

按第 9 节合并，不要直接强制推送。

### HTTPS 认证失败

确认使用平台支持的浏览器授权、凭据管理器或 Token，而不是账户登录密码。Token 只授予当前操作所需的最小仓库权限。

### 提示 LF/CRLF 转换

Windows 上出现换行符转换警告通常不是失败。项目已有 `.editorconfig` 约定 LF；不要因为该警告反复修改全部文件。

### 提交后才发现包含密码

仅删除文件再提交并不能从历史中移除秘密。应立即撤销或轮换已泄露密码/Token，并停止推送；随后再根据是否已经上传远程选择重写本地提交或清理远程历史。

## 11. 首次上传后的日常流程

每次开始工作前：

```powershell
git status
git pull --rebase
```

完成一组相关修改后：

```powershell
git status
git diff
git add 文件或目录
git diff --cached
git commit -m "feat(points): describe the change"
git push
```

推荐提交小而完整的变更，不要长期使用 `git add .` 而不检查暂存区。

在另一台电脑获取项目时使用：

```powershell
git clone <远程仓库地址>
Set-Location incentive-platform
Copy-Item .env.example .env
```

`.env` 不会随仓库同步，需要在每个开发环境单独创建。
