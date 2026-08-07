---
name: 偶得
description: 以公共服务台的清晰秩序承载账户与积分权益，并用暖黄细节保留偶遇与收获的温度。
colors:
  service-cobalt: "#174fa7"
  service-navy: "#102f65"
  harvest-yellow: "#f1c84a"
  paper: "#f8f7f2"
  workspace: "#e8edf3"
  white: "#ffffff"
  ink: "#17213a"
  muted-ink: "#697287"
  divider: "#d7dde6"
  success-ink: "#315e3f"
  success-surface: "#e2efe4"
  danger: "#a23843"
typography:
  display:
    fontFamily: "Noto Sans SC, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "40px"
    fontWeight: 800
    lineHeight: 1.3
    letterSpacing: "-0.03em"
  headline:
    fontFamily: "Noto Sans SC, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "34px"
    fontWeight: 700
    lineHeight: 1.25
    letterSpacing: "-0.025em"
  title:
    fontFamily: "Noto Sans SC, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "21px"
    fontWeight: 700
    lineHeight: 1.4
    letterSpacing: "-0.015em"
  body:
    fontFamily: "Noto Sans SC, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.7
    letterSpacing: "normal"
  label:
    fontFamily: "Noto Sans SC, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "13px"
    fontWeight: 600
    lineHeight: 1.4
    letterSpacing: "normal"
rounded:
  compact: "6px"
  control: "9px"
  panel: "12px"
  card: "16px"
  asymmetric-mark: "10px 10px 4px 10px"
spacing:
  xs: "6px"
  sm: "8px"
  md: "12px"
  lg: "18px"
  xl: "22px"
  2xl: "28px"
  3xl: "40px"
components:
  button-primary:
    backgroundColor: "{colors.service-cobalt}"
    textColor: "{colors.white}"
    typography: "{typography.label}"
    rounded: "{rounded.control}"
    height: "50px"
    width: "100%"
  button-secondary:
    backgroundColor: "{colors.workspace}"
    textColor: "{colors.muted-ink}"
    typography: "{typography.label}"
    rounded: "{rounded.control}"
    height: "46px"
  input:
    backgroundColor: "{colors.white}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.control}"
    padding: "0 14px"
    height: "50px"
  support-panel:
    backgroundColor: "{colors.workspace}"
    textColor: "{colors.ink}"
    rounded: "{rounded.panel}"
    padding: "25px"
  status-success:
    backgroundColor: "{colors.success-surface}"
    textColor: "{colors.success-ink}"
    typography: "{typography.label}"
    rounded: "{rounded.compact}"
    padding: "7px 10px"
  brand-mark:
    backgroundColor: "{colors.harvest-yellow}"
    textColor: "{colors.service-navy}"
    rounded: "{rounded.asymmetric-mark}"
    size: "38px"
---

# Design System: 偶得

## Overview

**Creative North Star: "有温度的权益服务厅"**

偶得的界面像一间经过精心整理的公共服务厅：身份、状态、权益和下一步操作都有明确位置，视觉秩序先帮助用户完成任务，再由少量暖色留下“偶遇—收获”的品牌记忆。它简约但不空旷，通过栏、柜台、分隔线和紧凑的信息组建立可信、可扫描的操作环境。

系统以深海军蓝提供制度感和品牌锚点，以钴蓝标记主要行动，以纸白和冷灰承载长时间操作。暖黄不是普遍装饰色，而是用于品牌标记、当前状态和权益强调的稀缺信号；整体保持克制、务实而不冷漠。

**Key Characteristics:**

- 桌面服务台式分区，结构清楚而信息密度适中。
- 深海军蓝、钴蓝与暖黄构成稳定且易识别的品牌三角。
- 中文无衬线字体、紧凑字号和清晰字重服务于扫描与操作。
- 轻圆角、细分隔线和有限阴影建立克制的层级。
- 不对称圆角标记是可复用的品牌签名。

## Colors

色彩体系以蓝色的可靠秩序为主，纸白与冷灰提供安静工作面，暖黄只在最值得注意的品牌或权益节点出现。

### Primary

- **服务钴蓝** (`service-cobalt`): 主要按钮、交互链接、输入焦点和关键图标的行动色。
- **服务深海军蓝** (`service-navy`): 品牌栏、侧栏和高权重结构面的锚定色。

### Secondary

- **收获暖黄** (`harvest-yellow`): 品牌标记、当前状态与权益强调；其稀缺性是识别力的一部分。

### Tertiary

- **确认绿** (`success-ink` / `success-surface`): 安全连接、完整度与成功反馈，只表达已确认的正向状态。
- **校验红** (`danger`): 表单错误文字与错误状态，不用于装饰。

### Neutral

- **纸白** (`paper`): 大面积内容底和安静的操作背景。
- **工作台冷灰** (`workspace`): 页面底、辅助面板和次级按钮。
- **纯白** (`white`): 输入面、主按钮文字和深色背景上的高对比内容。
- **深墨蓝** (`ink`): 标题与主要正文。
- **雾灰蓝** (`muted-ink`): 说明、辅助文案和低权重信息。
- **结构灰线** (`divider`): 标题区、列表、页脚之间的结构分隔。

### Named Rules

**The Warm Signal Rule.** 暖黄只标记品牌身份、当前选择或明确权益，不能扩散为大面积背景或普通按钮色。

**The Blue Responsibility Rule.** 深海军蓝负责结构与品牌，钴蓝负责行动；两者不可随意交换职责。

## Typography

**Display Font:** Noto Sans SC（回退为 PingFang SC、Microsoft YaHei、sans-serif）  
**Body Font:** Noto Sans SC（同一回退栈）

**Character:** 单一中文无衬线家族让界面保持公共服务场景所需的清晰与可信。层级主要依靠字重、字号和紧缩字距建立，不引入装饰字体。

### Hierarchy

- **Display**（800，40px，1.3）: 品牌承诺与极少量首屏主叙事；负字距让两行中文标题更凝练。
- **Headline**（700，34px，1.25）: 页面主任务、账户身份和关键数字标题。
- **Title**（700，21px，1.4）: 内容区与操作区的二级标题。
- **Body**（400，14px，1.7）: 表单输入、说明和常规阅读内容；品牌栏长说明可放宽至 1.85 行高。
- **Label**（600，13px，1.4）: 字段名、按钮、列表标签和权益短句；状态微文案可降到 11–12px 并提高字重。

### Named Rules

**The One-Family Rule.** 只用既定中文无衬线栈，通过层级而非字体混搭制造重点。

**The Dense-but-Breathable Rule.** 字号可以紧凑，但说明文字必须保留充足行高，避免“简约”变成难读或空旷。

## Layout

系统面向桌面端，当前实现以 1180px 为最小视口宽度。工作区使用明确的结构栏与内容区：登录入口采用固定 420px 品牌栏，右侧内容居中于最大 760px 的认证柜台；账户总览采用固定 248px 导航栏和最大约 1340px 的工作区。这些宽度属于各自表面组合，不应机械复制到所有页面。

内容布局使用可解释的网格：主任务与辅助说明并列，主内容与账户操作并列，关键身份或积分可拥有独立列。常用间距集中在 8–40px，局部内部节奏紧凑，区块之间明显放宽。分隔线承担结构切换，避免依赖大量卡片套卡片。

当前产品明确只支持桌面端；没有移动断点。新增桌面表面必须在 1180px 宽度下保持完整任务、状态与主要操作可见，不得把关键操作推到不可预期的横向滚动之外。

## Elevation & Depth

系统采用“结构平、行动浮”的混合策略。大多数区域依靠色块、栏位和细分隔线建立层级；主行动按钮获得柔和向下阴影，账户总览中的大型身份面板和独立内容容器可使用低对比环境阴影。焦点环是交互状态，不是装饰性发光。

### Shadow Vocabulary

- **主行动** (`0 9px 20px rgb(23 79 167 / 22%)`): 登录等主按钮的默认抬升；悬停时增强为 `0 12px 25px rgb(23 79 167 / 27%)`。
- **大型身份面** (`0 18px 40px rgb(31 54 94 / 16%)`): 只用于承载关键身份或积分总览的大型蓝色面板。
- **内容容器** (`0 10px 28px rgb(38 50 75 / 9%)`): 账户工作区中的独立纸白容器，不用于登录页的单层表单。
- **焦点环** (`0 0 0 3px rgb(23 79 167 / 14%)`): 输入聚焦反馈；按钮使用等价的 3px 外轮廓并保持 3px 间隔。

### Named Rules

**The Structure-First Rule.** 默认用分区、色差和 1px 分隔线建立层级；阴影只奖励主要行动或真正独立的高权重容器。

## Shapes

形态以轻度圆角为主：输入与按钮使用 9px，辅助面板使用 12px，大型账户容器使用 16px，小型状态使用 6px。品牌标记、权益勾选块和头像使用左上、右上、左下较圆、右下收紧的不对称轮廓，形成稳定签名。图标坚持细线、圆端点和圆连接，不混入厚重实心图标。

**The Asymmetric Signature Rule.** 不对称圆角只用于品牌、身份与权益标记；普通控件保持一致的对称圆角。

## Components

### Buttons

- **Shape:** 主控件使用轻柔而明确的 9px 圆角；登录主按钮高 50px，账户操作按钮至少高 46px。
- **Primary:** 服务钴蓝底、白字、半粗标签，全宽用于表单主提交，内容居中并可带 17px 线性箭头。
- **Hover / Focus:** 悬停上移 1px并略增强阴影；键盘焦点显示 3px 半透明钴蓝外轮廓；加载时保留尺寸、降低不透明度并显示旋转指示。
- **Secondary:** 冷灰底与灰蓝文字，不使用投影；只承载低于主操作一级的账户动作。
- **Text action:** 透明背景、钴蓝文字和箭头，悬停只推动箭头 3px，不把轻动作伪装成第二个主按钮。

### Cards / Containers

- **Corner Style:** 登录辅助面板为 12px；账户内容容器为 16px。
- **Background:** 辅助面板使用冷灰，内容容器使用纸白或纯白，品牌与身份总览可使用蓝色实底。
- **Shadow Strategy:** 登录表单保持单层无卡片阴影；账户工作区的独立容器才使用低对比环境阴影。
- **Border:** 结构切换优先使用 1px 冷灰分隔线。
- **Internal Padding:** 紧凑辅助面板约 25px，大型内容容器约 28–40px。

### Inputs / Fields

- **Style:** 50px 高的纯白输入面、1px 冷灰描边、9px 圆角，左右 14px 内边距；前导图标与输入之间保持 11px。
- **Focus:** 描边变为服务钴蓝，并出现 3px 低透明度焦点环；插入光标同样使用钴蓝。
- **Error / Disabled:** 错误使用红色描边、低透明红环和 11px 错误文字；按钮禁用保持布局并将不透明度降至 .72。

### Navigation

- **Style:** 固定深海军蓝侧栏，默认链接为浅灰蓝；链接以 13–14px 字号、约 10px 圆角和线性图标组成。
- **States:** 悬停使用更亮文字与深蓝层；当前项使用暖黄底与深海军蓝文字，清楚区别“可去往”与“正在这里”。

### Status Badges

- **Style:** 6px 圆角、11–12px 半粗文字、紧凑内边距。绿色组合只表达安全、完整或成功，不能承担普通分类标签。
- **State:** 状态必须由文字说明；圆点、颜色或图标只作为冗余提示。

### Brand Mark

- **Style:** 暖黄方块与深海军蓝字形，不对称圆角构成品牌签名；常用尺寸为 34–38px。
- **Usage:** 与“偶得”文字标并列，周围保留清晰空间；不得把该轮廓泛化到所有按钮和面板。

## Do's and Don'ts

### Do:

- **Do** 让账户状态、积分余额和主要行动在首个任务区域内清楚可见。
- **Do** 使用栏位、分隔线和有意义的色块组织信息，再考虑阴影。
- **Do** 把暖黄留给品牌、当前状态和权益节点，把钴蓝留给主要行动。
- **Do** 为表单提供可见标签、文字错误、加载反馈和键盘焦点状态。
- **Do** 对系统偏好减少动态效果，禁用进入动画和非必要过渡。

### Don't:

- **Don't** 把登录变成悬浮在空背景中央的通用卡片；入口必须同时传达品牌承诺与明确任务。
- **Don't** 用大面积暖黄、多个竞争主按钮或装饰性渐变稀释视觉职责。
- **Don't** 用阴影替代结构，也不要在登录单层表单外再套装饰卡片。
- **Don't** 擅自把演示用户、积分、奖品或业务规则写成真实产品事实。
- **Don't** 在没有新适配决策与验证前宣称支持移动端。
