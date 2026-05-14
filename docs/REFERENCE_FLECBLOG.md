# REFERENCE_FLECBLOG.md

> 鏈枃鑱岃矗锛氳褰?FlecBlog 鐨勬湰鍦板璁＄粨璁猴紝鏄庣‘鍝簺鍓嶇婧愮爜浼樺厛澶嶇敤銆佸摢浜涘悗绔兘鍔涘彧鍙傝€冦€佸摢浜涘唴瀹规殏涓嶇撼鍏ュ綋鍓嶄富绾裤€?

## 鏈湴鍙傝€冩簮

```text
D:\MyCode\ZBlogProject\ZBlog\_reference\FlecBlog
```

褰撳墠娴呭厠闅嗚嚜锛?

```text
https://github.com/talen8/FlecBlog
```

浠撳簱缁撴瀯锛?

```text
admin/      Vue 3 + Element Plus + Vite 绠＄悊绔?
blog/       Nuxt 4 鍗氬鍓嶅彴
server/     Go + Gin + GORM + PostgreSQL 鍚庣
installer/  瀹夎鍣?
hub/        鏂囨。绔?
panel/      闈㈡澘鐩稿叧
theme/      涓婚鐩稿叧
```

## FlecBlog 瀹樻柟瀹氫綅

FlecBlog 鏄笁绔垎绂诲崥瀹㈢郴缁燂細

- `server`锛欸o 1.25 / Gin / GORM / PostgreSQL锛?
- `admin`锛歏ue 3 / Element Plus / Vite锛?
- `blog`锛歂uxt 4.3.1 / Vue 3.5 / SCSS锛?
- 閲嶇偣鑳藉姏鍖呮嫭 SSR銆丼EO銆丼itemap銆丄tom Feed銆丮arkdown 娓叉煋銆佽瘎璁恒€佸弸閾俱€佺粺璁°€侀儴缃层€?

## Blog 绔紭鍏堝鐢?

浣嶇疆锛?

```text
_reference\FlecBlog\blog
```

浼樺厛澶嶇敤锛?

- Nuxt 4 宸ョ▼缁撴瀯锛?
- `app/pages` 鐨勫崥瀹㈣矾鐢变綋绯伙細棣栭〉銆佹枃绔犺鎯呫€佸垎绫汇€佹爣绛俱€佸綊妗ｃ€佸叧浜庛€佸弸閾俱€佺暀瑷€绛夛紱
- `app/components/features/article` 鐨勬枃绔犺鎯呯粍浠讹紱
- `app/components/features/comment` 鐨勮瘎璁虹粍浠讹紱
- `app/components/layouts` 鐨勫鑸€佷晶鏍忋€侀〉鑴氬拰椤甸潰妗嗘灦锛?
- `app/utils/markdown.ts`銆乣app/assets/css/_prose.scss` 鐨?Markdown 闃呰浣撻獙锛?
- `nuxt.config.ts` 涓?SEO銆丳WA銆佸浘鐗囥€佹€ц兘鐩稿叧閰嶇疆鎬濊矾銆?

灞€閮ㄤ慨鏀癸細

- 鍝佺墝銆佺珯鐐瑰悕銆丩ogo銆佷綔鑰呬俊鎭紱
- API 鍦板潃鍜屾帴鍙ｅ绾︼紱
- 涓嶉€傚悎褰撳墠鍗氬涓荤嚎鐨?ask銆乵oment銆丄I銆佽闃呯瓑椤甸潰鍏堥殣钘忔垨鍚庣疆锛?
- 瑙嗚椋庢牸鍙仛蹇呰娓呯悊锛屼笉绗竴闃舵澶ф敼鎴愪釜浜哄搧鐗屽畼缃戙€?

## Admin 绔紭鍏堝鐢?

浣嶇疆锛?

```text
_reference\FlecBlog\admin
```

浼樺厛澶嶇敤锛?

- Vue 3 + Vite + Element Plus 宸ョ▼缁撴瀯锛?
- `AdminLayout.vue`銆佷晶鏍忋€侀《閮ㄦ爮鍜岃矾鐢辩粨鏋勶紱
- `ArticleList.vue`銆乣ArticleForm.vue`銆丆odeMirror Markdown 缂栬緫鍣紱
- 鍒嗙被銆佹爣绛俱€佽瘎璁恒€佸弸閾俱€佹枃浠躲€佽缃€佺粺璁＄瓑鍚庡彴椤甸潰锛?
- `api/*.ts`銆乣types/*.ts` 鐨勬ā鍧楁媶鍒嗘柟寮忥紱
- 鍥剧墖涓婁紶銆佺瓫閫夐潰鏉裤€侀€氱敤鍒楄〃绛夌粍浠躲€?

灞€閮ㄤ慨鏀癸細

- 閫傞厤 Java 鍚庣 API锛?
- 娓呯悊 FlecBlog 鍝佺墝锛?
- 鍒濇湡闅愯棌 AI銆乵oment銆丷SS 璁㈤槄銆佸弽棣堢瓑闈炲綋鍓嶄富绾挎ā鍧楋紱
- 鍚庡彴淇濈暀姝ｅ紡 CMS 璐ㄦ劅锛屼笉鏀规垚涓存椂宸ュ叿椤点€?

## Server 绔彧鍙傝€?

浣嶇疆锛?

```text
_reference\FlecBlog\server
```

鍙弬鑰冿細

- 鏂囩珷銆佸垎绫汇€佹爣绛俱€佽瘎璁恒€佸弸閾俱€佹枃浠躲€佽缃€佺粺璁°€佺敤鎴风瓑棰嗗煙妯″瀷锛?
- `api/v1` 璺敱缁勭粐锛?
- PostgreSQL 鍒濆鍖栬剼鏈拰杩佺Щ鑺傚锛?
- JWT銆丱Auth銆佷笂浼犮€侀€氱煡銆佸畾鏃朵换鍔°€丷SS/Atom銆丼wagger锛?
- Dockerfile銆乨ocker-compose 鍜岀幆澧冨彉閲忛厤缃柟寮忋€?

涓嶅鐢細

- Go/Gin/GORM 浠ｇ爜涓嶈繘鍏?ZBlog 鍚庣锛?
- 涓嶅仛鏈烘 Java 缈昏瘧锛?
- AI銆丮CP銆丗eishu銆佸井淇°€乵oment 绛夋墿灞曡兘鍔涘厛鍚庣疆銆?

## 绗竴闃舵鍚告敹娓呭崟

绗竴闃舵蹇呴』鍚告敹锛?

- 鍗氬鍓嶅彴 Nuxt SSR/SEO 鏋舵瀯锛?
- 绠＄悊鍚庡彴姝ｅ紡 CMS 缁撴瀯锛?
- 鏂囩珷 Markdown 缂栬緫鍜岄槄璇讳綋楠岋紱
- 鍒嗙被銆佹爣绛俱€佸綊妗ｃ€佽瘎璁恒€佸弸閾俱€佹枃浠躲€佽缃€佺粺璁★紱
- Docker 閮ㄧ讲鍜岀幆澧冨彉閲忓垎灞傘€?

绗竴闃舵鏆傜紦锛?

- 寮轰釜浜哄搧鐗岄椤碉紱
- 椤圭洰灞曠ず锛?
- DevWiki Studio锛?
- 鐩稿唽銆佽璇达紱
- AI 鎽樿銆丮CP銆佸鏉傞€氱煡闆嗘垚锛?
- 涓€閿畨瑁呭櫒銆?

