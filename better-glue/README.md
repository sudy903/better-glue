# Better Glue

**Create-Fly 的附属 mod**：加一个「胶水查看器」，手持时把已被 Create 强力胶圈出的区域用贴图渲染出来，
方便看清哪一块已经粘在一起。

> 它**不新增任何胶水**——粘接行为、区域数据全部来自 Create-Fly 的 `create:super_glue`。
> 本 mod 只接管了那个实体在客户端的渲染。

## 依赖

| 依赖 | 版本 |
|---|---|
| Minecraft | 1.21.10 |
| Fabric Loader | ≥ 0.20.0 |
| Fabric API | 1.21.10 版本 |
| **Create-Fly** | 6.0.9（mod id `create`，硬依赖，没装则不加载） |

## 用法

1. 用 Create 的**强力胶**（`create:super_glue`）右键两个方块，粘出一块区域
2. 手持**胶水查看器**（主手或副手都行）→ 所有已粘连区域被胶水贴图覆盖并持续显示
3. 收起查看器 → 立即消失

合成：玻璃板 + Create 强力胶（无序）。也可 `/give @s better-glue:glue_viewer`。

## 实现要点

- **只注册一个物品**，没有任何自定义实体/方块。
- 客户端在 `onInitializeClient` 里查 `Registries.ENTITY_TYPE.get(Identifier.of("create","super_glue"))`，
  拿到后调用 `EntityRendererFactories.register(...)` 覆盖它的渲染器。
  Create-Fly 自己那个渲染器是空实现（`shouldRender` 恒 false，它用 Outliner 画框），覆盖没有视觉损失。
  初始化顺序由 `fabric.mod.json` 里的 `"create": "*"` 依赖保证：Create 先加载，我们后注册，覆盖生效。
- 渲染直接用 Create 自己的 `create:textures/special/glue.png`，看起来和 Create 原版高亮一致。
- 每个胶水实体的 AABB 就是区域范围，直接取 `entity.getBoundingBox()`；
  通过 `OrderedRenderCommandQueue.submitCustom` 提交 6 个面，正反绕序各一次靠背面剔除去重，
  盒子外扩 0.005 避免与方块表面 z-fighting。

## 代码结构

```
src/main/java/sudy/sudy/
├─ BetterGlue.java              入口：注册物品、进工具页签
└─ content/
   ├─ ModContent.java           只注册 glue_viewer 一个物品
   └─ GlueViewerItem.java       空壳物品（只有 tooltip）

src/client/java/sudy/sudy/client/
├─ BetterGlueClient.java        查 create:super_glue 并覆盖其渲染器
└─ render/
   └─ SuperGlueEntityRenderer.java  手持查看器时渲染胶水区域
```

## 构建

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1
# 或
./gradlew build
```

产物：`build/libs/better-glue-1.0.0.jar`（remap 后的生产 jar，直接丢进 `.minecraft\mods`）。

CI：`.github/workflows/build.yml`，push 后自动构建并上传 Artifacts。注意工程在仓库的 `better-glue/` 子目录，
workflow 里用 `working-directory: better-glue`；`path:` 也要带 `better-glue/` 前缀。

## 已知限制

- 只画**盒子表面**，没有 Create 那种描边线框。
- 贴图 UV 是每个面 0~1，图案会被拉伸到整个面；想"按方块重复"需要改成按尺寸重复的 UV。
- 只能看到已加载（在追踪范围内）的胶水实体，`maxTrackingRange` 由 Create 设为 10 区块。

## 许可

物品贴图是本仓库现画的 16×16 PNG；渲染时读取的是 Create 自己包内的贴图。
