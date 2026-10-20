# 拼音乐园 - GitHub 自动构建指南

## 🚀 一键构建 APK（无需本地环境）

### 步骤 1: 创建 GitHub 仓库

1. 登录 GitHub: https://github.com
2. 点击右上角 `+` → `New repository`
3. 填写仓库信息:
   - **Repository name**: `pinyin-park`（或你喜欢的名字）
   - **Description**: 拼音乐园 - 趣味拼音学习 App
   - **Private/Public**: 选择 Public（公开）或 Private（私有）
4. 点击 `Create repository`

### 步骤 2: 推送代码

运行项目中的 `push-to-github.bat` 脚本，或手动执行:

```bash
cd c:\Users\Administrator\WorkBuddy\拼音
git remote add origin https://github.com/YOUR_USERNAME/pinyin-park.git
git push -u origin master
```

### 步骤 3: 查看构建状态

1. 打开你的 GitHub 仓库页面
2. 点击 `Actions` 标签页
3. 等待构建完成（约 3-5 分钟）
4. 构建完成后会显示 ✓ 绿色勾

### 步骤 4: 下载 APK

1. 点击构建任务（如 "Android CI/CD"）
2. 点击右侧的 `app-debug` 或 `pinyin-park-apk`
3. 点击 `Download` 下载 APK 文件
4. 将 APK 传输到手机安装

---

## 🔧 GitHub Actions 自动构建流程

```yaml
1. 检出代码 (actions/checkout@v4)
       ↓
2. 设置 JDK 17 (actions/setup-java@v4)
       ↓
3. 设置 Android SDK (android-actions/setup-android@v2)
       ↓
4. 执行 Gradle 构建 (./gradlew assembleDebug)
       ↓
5. 上传 APK 作为 Artifact
```

---

## 📱 安装 APK 到手机

APK 下载完成后：
1. 将 APK 文件传输到手机（通过微信、QQ、U盘等方式）
2. 在手机上打开文件管理器
3. 点击 APK 文件安装
4. 如果提示"禁止安装未知来源应用"：
   - 进入设置 → 安全 → 允许未知来源
   - 或在安装弹窗中点击"允许"

---

## ❓ 常见问题

### Q: 推送代码时认证失败？
**解决方案**：
1. 创建 Personal Access Token: https://github.com/settings/tokens
2. 设置凭据：
```bash
git remote set-url origin https://你的TOKEN@github.com/handyily/pinyin-park.git
```

### Q: Actions 构建失败？
**检查项**：
- 代码是否有语法错误
- Gradle 配置是否正确
- 可以点击失败的构建查看日志

### Q: 下载的 APK 无法安装？
**检查项**：
- 手机系统 >= Android 6.0
- 已开启"允许未知来源应用"
- APK 完整性是否正常

---

## 🔑 使用固定密钥发布 APK（GitHub Release）

release 包必须使用**同一把密钥**签名，否则新版本无法覆盖安装到旧版本之上。
[`.github/workflows/android-release.yml`](../.github/workflows/android-release.yml) 从仓库 Secrets 读取密钥来签名，密钥本身**不入库**。

### 步骤 1：生成密钥（只需一次）

密钥已生成在 `android/keystore/release.jks`（已被 `.gitignore` 忽略）。如需重新生成：

```powershell
keytool -genkeypair -v -keystore android/keystore/release.jks `
  -alias pinyinpark -keyalg RSA -keysize 2048 -validity 10000 `
  -storetype PKCS12 -storepass <密钥库密码> -keypass <密钥密码> `
  -dname "CN=PinyinPark, OU=Mobile, O=PinyinPark, L=Beijing, ST=Beijing, C=CN"
```

转成单行 base64（用于 Secret）：

```powershell
$b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes("android/keystore/release.jks"))
Set-Content "android/keystore/release.jks.base64.txt" $b64 -NoNewline -Encoding ascii
```

> ⚠️ 密钥文件与密码务必自行备份，丢失后无法为已发布的应用签名新版本。

### 步骤 2：配置 4 个 Secrets

仓库 → `Settings` → `Secrets and variables` → `Actions` → `New repository secret`：

| Secret 名称 | 值 |
|---|---|
| `KEYSTORE_BASE64` | `release.jks.base64.txt` 的完整内容（单行，无换行） |
| `KEYSTORE_PASSWORD` | 密钥库密码 |
| `KEY_ALIAS` | `pinyinpark` |
| `KEY_PASSWORD` | 密钥密码（PKCS12 格式与密钥库密码相同） |

已安装 [GitHub CLI](https://cli.github.com/) 时也可用命令行：

```powershell
gh secret set KEYSTORE_BASE64 --repo taoistwar/pinyin-park < android/keystore/release.jks.base64.txt
gh secret set KEYSTORE_PASSWORD --repo taoistwar/pinyin-park
gh secret set KEY_ALIAS --repo taoistwar/pinyin-park --body "pinyinpark"
gh secret set KEY_PASSWORD --repo taoistwar/pinyin-park
```

### 步骤 3：打标签触发发布

```bash
git tag v1.0.0
git push origin v1.0.0
```

也可在 `Actions` → `Android Release` → `Run workflow` 手动执行并填写版本号。

工作流会依次完成：解析版本号 → 写入密钥 → `assembleRelease` → `apksigner verify` 校验签名 → 创建 GitHub Release 并上传 `pinyin-park-v1.0.0.apk` 与 `mapping.txt`。

`versionCode` 由标签自动推导（`1.2.3` → `10203`），每次发版都会递增，保证手机端可覆盖升级。

### 步骤 4：本地签名构建（可选）

`android/keystore.properties`（已被忽略）内容示例：

```properties
storeFile=keystore/release.jks
storePassword=<密钥库密码>
keyAlias=pinyinpark
keyPassword=<密钥密码>
```

然后执行 `cd android && ./gradlew assembleRelease`，产物在 `android/app/build/outputs/apk/release/app-release.apk`。
未提供密钥时，release 任务只产出未签名 APK，不会中断构建。

---

## 🎯 完成后

恭喜！你现在拥有：
- ✅ GitHub 仓库备份
- ✅ 自动构建系统
- ✅ 可下载的 APK 安装包
- ✅ 每次推送自动重新构建
- ✅ 固定密钥签名的 Release APK
