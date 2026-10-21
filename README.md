# AI 绘图助手

一个前后端分离的 AI 图片生成与编辑平台。支持文生图、本地图片改图、可选遮罩局部修改以及公网图片 URL 图生图。浏览器只调用自建 Spring Boot API；第三方图片接口密钥仅保存在后端环境变量中。生成结果会被解码、重新编码为 PNG，按日期保存到本地目录，并将相对访问路径写入 MySQL。

## 1. 整体架构

```text
┌──────────────────────────────┐
│ Vue 3 + Vite + Element Plus  │
│ 提示词 / 原图 / 遮罩 / 结果 / 历史 │
└──────────────┬───────────────┘
               │ REST / JSON
               ▼
┌──────────────────────────────┐
│ Spring Boot 3 / Java 17      │
│ Controller → Service         │
│ 参数校验 / 上传校验 / API 代理    │
└───────┬───────────┬──────────┘
        │           │
        │           ├──────────────► OpenAI 兼容生图接口
        │           │                Bearer API Key（仅后端）
        │           │
        ▼           ▼
┌─────────────┐  ┌────────────────────────┐
│ MySQL 8     │  │ uploads/images/yyyy/MM/dd │
│ 图片元数据   │  │ PNG 文件                │
└─────────────┘  └────────────────────────┘
```

### 生成流程

1. 文生图提交 `prompt / size / quality` 到 `POST /api/image/generate`。
2. 图生图可提交本地 `image / mask`，或提交公网 `imageUrl`。
3. 后端校验参数和上传文件，使用服务端 `IMAGE_API_KEY` 调用第三方接口。
4. 读取 `data[0].b64_json`，Base64 解码并通过 `ImageIO` 重新编码为 PNG。
5. 文件保存到 `uploads/images/yyyy/MM/dd/{uuid}.png`。
6. MySQL 只保存结果图片 URL、提示词、模型和生成参数，不保存 Base64。
7. 后端返回图片 URL 和生成耗时，前端刷新本次结果与历史画廊。

### 扩展边界

- 用户登录：在 Controller 前增加 Spring Security/JWT，历史表增加 `user_id`。
- 收藏、分享、风格：新增领域表及独立 Service，不需要修改图片生成客户端。
- Prompt 模板与优化：新增 Prompt 模块，在调用 `ImageService` 前完成模板合成或模型优化。
- OSS：抽取 `ImageStorage` 接口，将当前本地文件实现替换为 S3/OSS/COS 实现。

## 2. 项目结构

```text
ai-image-assistant/
├─ backend/
│  ├─ pom.xml
│  └─ src/main/
│     ├─ java/com/example/aiimage/
│     │  ├─ common/       # 统一响应
│     │  ├─ config/       # MyBatis、HTTP、CORS、静态资源配置
│     │  ├─ controller/   # REST API
│     │  ├─ dto/          # 内外部接口 DTO
│     │  ├─ entity/       # 数据库实体
│     │  ├─ exception/    # 全局异常处理
│     │  ├─ mapper/       # MyBatis-Plus Mapper
│     │  ├─ service/      # 图片业务逻辑
│     │  └─ utils/        # Base64 与 PNG 文件处理
│     └─ resources/application.yml
├─ database/schema.sql
└─ frontend/
   ├─ src/
   │  ├─ api/             # Axios 与图片 API
   │  ├─ components/      # 历史图片卡片
   │  ├─ router/          # Vue Router
   │  ├─ types/           # TypeScript 类型
   │  ├─ utils/           # 图片 URL 处理
   │  └─ views/ImageGenerator.vue
   └─ vite.config.ts
```

## 3. 数据库

要求 MySQL 8+。执行：

```powershell
mysql -uroot -p < database/schema.sql
```

脚本会创建 `ai_image` 数据库和 `image_history` 表。字段包括：`id`、`prompt`、`image_url`、`model`、`size`、`quality`、`create_time`。

## 4. 后端启动

### 环境要求

- JDK 17
- Maven 3.9+
- MySQL 8+

### 配置环境变量

PowerShell 示例：

```powershell
$env:IMAGE_API_KEY="替换为真实密钥"
$env:DB_URL="jdbc:mysql://localhost:3306/ai_image?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="你的数据库密码"
```

可选变量：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `IMAGE_BASE_URL` | `https://api.likeai520.cc/v1/images/generations` | 文生图接口 |
| `IMAGE_EDIT_URL` | `https://api.likeai520.cc/v1/images/edits` | 改图 / 图生图接口 |
| `IMAGE_EDIT_API_KEY` | 回退到 `IMAGE_API_KEY` | 改图服务使用不同供应商时可单独配置 |
| `IMAGE_MODEL` | `gpt-image-2` | 模型名 |
| `IMAGE_UPLOAD_DIR` | `uploads/images` | 文件保存根目录 |
| `IMAGE_MAX_BYTES` | `26214400` | 单张上游图片最大字节数 |
| `IMAGE_MAX_UPLOAD_BYTES` | `20971520` | 单张原图或遮罩最大字节数 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | 前端来源，多个值用逗号分隔 |
| `SERVER_PORT` | `8080` | 后端端口 |

启动：

```powershell
cd backend
mvn spring-boot:run
```

API Key 未写入源码或前端。生产环境应通过系统环境变量、容器 Secret 或密钥管理服务注入。

## 5. 前端启动

要求 Node.js 18+（推荐 Node.js 20）。

```powershell
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`。开发环境由 Vite 将 `/api` 和 `/uploads` 代理到 `http://localhost:8080`。

生产环境前后端分离部署时，复制 `.env.example` 为 `.env.production`，将 `VITE_API_BASE_URL` 设置为后端公网 Origin，然后执行：

```powershell
npm run build
```

## 6. REST API

### 生成图片

`POST /api/image/generate`

```json
{
  "stylePrompt": "统一使用电影级概念艺术风格，冷色调，高细节",
  "prompt": "一只白色猫坐在窗边",
  "size": "1024x1024",
  "quality": "low"
}
```

成功响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "imageUrl": "/uploads/images/2026/10/14/xxxxxxxx.png",
    "elapsedMillis": 12345
  }
}
```

### 查询历史

`GET /api/image/history?page=1&size=12`

返回 MyBatis-Plus 分页对象，记录按 `create_time` 倒序排列。

### 本地图片改图

`POST /api/image/edit`，请求类型为 `multipart/form-data`。

| 字段 | 必填 | 说明 |
|---|---|---|
| `stylePrompt` | 否 | 统一风格提示词，最多 2000 字符 |
| `prompt` | 是 | 修改要求 |
| `image` | 是 | 原图，支持 PNG/JPG/WebP，最大 20MB |
| `mask` | 否 | 遮罩图，用于局部修改 |
| `size` | 是 | 当前为 `1024x1024` |
| `quality` | 是 | `low` / `medium` / `high` |

支持的尺寸：

| 尺寸 | 比例与用途 |
|---|---|
| `1024x1024` | 1:1 正方形，适合头像、封面和通用配图 |
| `1024x1536` | 2:3 竖版，适合海报、人像和商品图 |
| `1536x1024` | 3:2 横版，适合摄影和文章头图 |
| `1152x2048` | 9:16，适合手机壁纸和短视频画面 |
| `2048x1152` | 16:9，适合视频封面和桌面壁纸 |

最终是否支持某个尺寸还取决于所配置的第三方模型服务；若供应商限制尺寸，会返回对应的上游错误。

### 图片 URL 改图

`POST /api/image/edit-by-url`

```json
{
  "stylePrompt": "统一使用国潮插画风格，红金配色，高细节",
  "prompt": "保留主体，把图片改成国潮海报风格",
  "imageUrl": "https://example.com/input.png",
  "size": "1024x1024",
  "quality": "low"
}
```

三种生成方式都支持可选的 `stylePrompt`。后端会将其与本次 `prompt` 统一组装后再发送给上游模型；历史记录仍只保存本次图片描述或修改要求，避免展示重复的长风格文本。

### 删除历史

`DELETE /api/image/{id}`

删除数据库记录，并尝试同步删除对应本地图片文件。

## 7. 接口测试

PowerShell：

```powershell
$body = @{
  prompt = "一个漂浮在天空中的机械城市，电影级概念艺术"
  size = "1024x1024"
  quality = "low"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/image/generate" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body

Invoke-RestMethod -Uri "http://localhost:8080/api/image/history?page=1&size=12"
Invoke-RestMethod -Uri "http://localhost:8080/api/image/1" -Method Delete
```

本地图片改图：

```powershell
curl.exe -X POST "http://localhost:8080/api/image/edit" `
  -F "prompt=把这张图改成国潮海报风格，保留主体" `
  -F "image=@D:\images\input.png" `
  -F "size=1024x1024" `
  -F "quality=low"
```

使用公网图片 URL 改图：

```powershell
$editBody = @{
  prompt = "保留主体，改成水彩风格"
  imageUrl = "https://example.com/input.png"
  size = "1024x1024"
  quality = "low"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/image/edit-by-url" `
  -Method Post `
  -ContentType "application/json" `
  -Body $editBody
```

后端单元测试：

```powershell
cd backend
mvn test
```

前端类型检查与生产构建：

```powershell
cd frontend
npm run type-check
npm run build
```

## 8. 异常与安全设计

- Bean Validation 校验提示词、尺寸、质量和分页参数。
- `GlobalExceptionHandler` 统一返回 `{ code, message, data }`。
- 第三方接口连接与读取均设置超时，失败返回 502。
- API Key 只在后端请求头中使用，不进入数据库、日志和前端包。
- 图片大小受限；无效 Base64 或无法解析的图片不会落盘。
- 上传原图和遮罩限制为 PNG/JPG/WebP，单张最大 20MB。
- 文件路径经过根目录校验，避免目录穿越。
- 数据库写入失败时会删除刚生成的文件，减少孤儿文件。
