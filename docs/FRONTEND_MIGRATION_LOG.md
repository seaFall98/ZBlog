# FRONTEND_MIGRATION_LOG.md

> 鏈枃鑱岃矗锛氳褰?FlecBlog 鍓嶇婧愮爜杩佸叆 ZBlog 鐨勫熀绾跨姸鎬併€佹潵婧愯矾寰勩€佸綋鍓嶆湭淇敼鍐呭鍜屽悗缁€傞厤姝ラ銆?

## 杩佸叆鏃堕棿

2026-05-14

## 鏉ユ簮

```text
D:\MyCode\ZBlogProject\ZBlog\_reference\FlecBlog\blog
D:\MyCode\ZBlogProject\ZBlog\_reference\FlecBlog\admin
```

## 鐩爣

```text
D:\MyCode\ZBlogProject\ZBlog\blog
D:\MyCode\ZBlogProject\ZBlog\admin
```

## 褰撳墠鐘舵€?

鏈杩佸叆淇濇寔 FlecBlog 鍓嶇婧愮爜鍘熻矊锛屼笉鍋氬姛鑳芥敼閫犮€佷笉鍋氬搧鐗屾浛鎹€佷笉鍋?Java API 閫傞厤銆?

杩欐牱鍋氱殑鍘熷洜锛?

- 鍏堜繚鐣欏彲杩愯鍩虹嚎锛?
- 鍚庣画姣忎釜鏀瑰姩閮芥湁娓呮櫚 diff锛?
- 閬垮厤绗竴姝ュ氨鎶婃簮鐮佹敼涔憋紱
- 绗﹀悎鈥滃彲澶嶇敤灏卞厛鐢紝鍐嶅眬閮ㄤ慨鏀光€濈殑绛栫暐銆?

## 鍚庣画閫傞厤椤哄簭

1. 楠岃瘉 `blog` 鍜?`admin` 渚濊禆瀹夎銆佺被鍨嬫鏌ュ拰鏋勫缓锛?
2. 娓呯悊鍝佺墝淇℃伅鍜岄粯璁ょ珯鐐规枃妗堬紱
3. 闅愯棌褰撳墠鍗氬涓荤嚎涓嶉渶瑕佺殑鍏ュ彛锛?
4. 璁捐 Java API adapter锛?
5. 鐢?Java 鍚庣鏇挎崲 FlecBlog Go API锛?
6. 杩佺Щ鏃?ZBlog 涓洿寮虹殑 Markdown/鏂囩珷璇︽儏灞€閮ㄨ兘鍔涳紱
7. 娴忚鍣ㄦ埅鍥鹃獙鏀朵富瑕侀〉闈€?

## 褰撳墠涓嶅仛

- 涓嶉噸璁捐棣栭〉锛?
- 涓嶆妸椤圭洰鏀规垚涓汉鍝佺墝瀹樼綉锛?
- 涓嶆帴鍏?DevWiki Studio锛?
- 涓嶅鐞嗙浉鍐屻€佽璇淬€佷竴閿畨瑁呭櫒锛?
- 涓嶄慨鏀?`_reference` 鐩綍銆?

## 楠岃瘉璁板綍

2026-05-14锛?

- `admin`: `npm install` 鎴愬姛銆?
- `admin`: `npm run type-check` 鎴愬姛銆?
- `admin`: `npm run build` 鎴愬姛銆?
- `blog`: 绗竴娆?`npm install` 澶辫触锛宍package-lock.json` 涓殑 resolved 鍦板潃鎸囧悜鑵捐 npm 闀滃儚锛屼笅杞?`whatwg-url` 杩斿洖 `E567 Unknown Status`銆?
- `blog`: 浣跨敤 `--registry=https://registry.npmjs.org/` 浠嶈鍙?lock 涓吘璁暅鍍忓湴鍧€锛屼笅杞?`serialize-javascript` 杩斿洖 `E567 Unknown Status`銆?
- `blog`: 浣跨敤 `--package-lock=false --registry=https://registry.npmjs.org/` 鏈珛鍗虫姤閿欙紝浣嗚秴杩?5 鍒嗛挓浠嶆湭鐢熸垚 `node_modules`锛屾畫鐣?npm 杩涚▼宸插仠姝€?
- `blog`: 灏?`blog/package-lock.json` 涓?`https://mirrors.cloud.tencent.com/npm/` 鎵归噺鏇挎崲涓?`https://registry.npmjs.org/` 鍚庯紝`npm install --registry=https://registry.npmjs.org/` 鎴愬姛銆?
- `blog`: `npm run type-check` 鎴愬姛锛屼絾杈撳嚭 Vue/Volar 鎻掍欢瑙ｆ瀽璀﹀憡锛歚vue-router/volar/sfc-route-blocks` 鎵句笉鍒?`@vue/language-core`锛屽懡浠ら€€鍑虹爜浠嶄负 0銆?
- `blog`: `npm run build` 鎴愬姛锛岃緭鍑?Nuxt sourcemap銆丯ode deprecated exports pattern銆乻harp win32-x64 鏋舵瀯绛夎鍛婏紝鍛戒护閫€鍑虹爜涓?0銆?

鍚庣画澶勭悊锛?

- 妫€鏌ユ槸鍚﹂渶瑕佺粰 `blog` 鏄惧紡鍔犲叆 `vue-tsc` 浠ユ秷闄?type-check 鏈熼棿鐨?npx/Volar 璀﹀憡锛?
- 鍚庣画閮ㄧ讲鍒?Linux 鏈嶅姟鍣ㄦ椂锛屾敞鎰?Nuxt build 鎻愮ず鐨?`sharp` 鏋舵瀯淇℃伅锛屽簲鍦ㄧ洰鏍囨灦鏋勪笂閲嶆柊鏋勫缓闀滃儚锛?
- 褰撳墠鍙０鏄庣殑鑼冨洿浠呴檺锛欶lecBlog 鍓嶇婧愮爜宸茶縼鍏ワ紝`admin` 鍜?`blog` 鍧囧畬鎴愭湰鍦颁緷璧栧畨瑁呫€佺被鍨嬫鏌ュ拰鐢熶骇鏋勫缓銆?
