# AGENTS.md

> 鏈枃鑱岃矗锛氬畾涔?Codex 鍦?ZBlog 涓殑宸ヤ綔瑙勫垯銆佸繀璇绘枃妗ｃ€佺姝簨椤广€侀獙璇佽姹傚拰闃舵鎺ㄨ繘鏂瑰紡銆?

## Prime Rule

浠讳綍浠诲姟瀹屾垚澹版槑蹇呴』鏈夊綋鍓嶄細璇濈殑鏂伴矞楠岃瘉璇佹嵁銆傛病鏈夎繍琛岄獙璇佸懡浠わ紝灏变笉鑳借宸茬粡瀹屾垚鎴栭€氳繃銆?

## Required Reading

姣忔 ZBlog 浠诲姟寮€濮嬪墠锛屽厛闃呰锛?

```text
README.md
AGENTS.md
docs/0_PROJECT_CONTEXT.md
docs/REFERENCE_FLECBLOG.md
docs/REUSE_ASSET_AUDIT.md
docs/PRODUCT_PLAN.md
docs/FEATURE_LIST.md
docs/FRONTEND_PLAN.md
docs/BACKEND_PLAN.md
docs/API_PLAN.md
docs/DATA_MODEL_PLAN.md
docs/TECH_STACK_PLAN.md
docs/IMPLEMENTATION_ROADMAP.md
docs/CODEX_WORKFLOW.md
docs/FRONTEND_MIGRATION_LOG.md
```

濡傛灉浠诲姟娑夊強鍓嶇杩佺Щ锛岃繕蹇呴』鍏堟鏌ワ細

```text
D:\MyCode\ZBlogProject\ZBlog\_reference\FlecBlog\blog
D:\MyCode\ZBlogProject\ZBlog\_reference\FlecBlog\admin
```

濡傛灉浠诲姟娑夊強 Markdown銆佹枃绔犺鎯呮垨 Java 鍚庣鏂囩珷妯″瀷锛岃繕蹇呴』妫€鏌ワ細

```text
D:\MyCode\ZBlogProject\ZBlog
```

## Hard Prohibitions

- 涓嶈鎶婂綋鍓嶄富绾挎敼鎴愪釜浜哄搧鐗屽畼缃戙€侀」鐩泦鎴?DevWiki Studio銆?
- 涓嶈鎶婇」鐩檷绾ф垚绠€鍗?CRUD Demo銆?
- 涓嶈鎶?FlecBlog 鍚庣 Go 浠ｇ爜鏈烘缈昏瘧鎴?Java銆?
- 涓嶈鍦ㄦ病鏈夎縼绉讳换鍔＄殑鎯呭喌涓嬬洿鎺ュぇ瑙勬ā澶嶅埗婧愮爜銆?
- 涓嶈涓㈠純 FlecBlog 鍓嶇鍙鐢ㄧ粨鏋勫悗閲嶆柊鍙戞槑椤甸潰銆?
- 涓嶈鐩茬洰鐓ф惉鏃?ZBlog/Klee 鐨勮瑙夊拰璺嚎鎽囨憜銆?
- 涓嶈鎶?`_reference/` 閲岀殑婧愮爜褰撲綔涓€鏂逛笟鍔′唬鐮佺洿鎺ヤ慨鏀广€?
- 涓嶈鎻愪氦瀵嗛挜銆佹湇鍔″櫒瀵嗙爜銆佸煙鍚嶈瘉涔︺€佺湡瀹?`.env`銆?

## Feat Shape

鍚庣画姣忎釜闃舵浠诲姟鎸夋鏍煎紡鎺ㄨ繘锛?

```text
feat/<name>
Goal:
Scope:
Inputs:
Implementation steps:
Verification commands:
Done definition:
Status:
```

`Done` 蹇呴』婊¤冻锛?

- 瀹炵幇瀹屾垚锛?
- 楠岃瘉鍛戒护杩愯閫氳繃锛?
- 鏂囨。鍚屾鏇存柊锛?
- 閬楃暀闂鏄庣‘璁板綍锛?
- 娌℃湁娣峰叆鏃犲叧鏀瑰姩銆?

## Verification Rules

鏂囨。浠诲姟锛?

```powershell
Test-Path docs
Get-ChildItem docs -File
Select-String -Path docs\*.md -Pattern "鏈枃鑱岃矗"
git status --short
```

鍓嶇浠诲姟锛?

```powershell
npm install
npm run type-check
npm run build
```

鍚庣浠诲姟锛?

```powershell
mvn test
mvn package
```

閮ㄧ讲浠诲姟锛?

```powershell
docker compose config
```
