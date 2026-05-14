# PHASE3_CONTENT_CLOSED_LOOP_LOG.md

> 鏈枃鑱岃矗锛氳褰?Phase 3 鏂囩珷绯荤粺闂幆鐨勫疄闄呰惤鍦扮粨鏋溿€侀獙璇佸懡浠ゃ€佸綋鍓嶈竟鐣屽拰涓嬩竴闃舵寤鸿銆?

## 鏈钀藉湴

鏃堕棿锛?026-05-15 鏈湴鎵ц璁板綍銆?

宸插畬鎴愶細

- 鍚庣缁熶竴鍝嶅簲浠庡瓧绗︿覆鐮佽皟鏁翠负 FlecBlog 鍓嶇鍏煎鐨勬暟鍊肩爜锛氭垚鍔?`code=0`銆?
- 鐧诲綍鍝嶅簲鏀逛负 `access_token`銆乣token_type`銆乣expires_in`锛屽吋瀹瑰悗鍙扮櫥褰曢〉銆?
- 寮曞叆 `spring-boot-starter-jdbc`銆丗lyway PostgreSQL 鏀寔鍜屾祴璇曠敤 H2 PostgreSQL mode銆?
- 鍒涘缓 Flyway baseline锛歚categories`銆乣tags`銆乣articles`銆乣article_tags`銆乣menus`銆?
- 鍔犲叆 baseline 绉嶅瓙鏁版嵁锛氶粯璁ゅ垎绫汇€侀粯璁ゆ爣绛俱€佺ず渚嬫枃绔犮€侀《閮?搴曢儴鑿滃崟銆?
- 鎸夎交閲?DDD 缁撴瀯钀藉湴 `site`銆乣taxonomy`銆乣content` 妯″潡銆?
- 瀹炵幇鍏紑 API锛氳彍鍗曘€佸垎绫汇€佹爣绛俱€佹枃绔犲垪琛ㄣ€佹枃绔犺鎯呫€侀殢鏈烘枃绔?slug銆?
- 瀹炵幇鍚庡彴 API锛氬垎绫?CRUD銆佹爣绛?CRUD銆佹枃绔犲垪琛?璇︽儏/鍒涘缓/鏇存柊/鍒犻櫎/鍙戝竷/鍙栨秷鍙戝竷銆?
- 瀹炵幇鍩虹 Markdown 娓叉煋蹇収锛歚content_markdown`銆乣content_html`銆乣content_text`銆?

## 宸查獙璇?

```powershell
cd D:\MyCode\ZBlogProject\ZBlog\server
mvn test
mvn package

cd D:\MyCode\ZBlogProject\ZBlog\blog
npm run type-check
npm run build

cd D:\MyCode\ZBlogProject\ZBlog\admin
npm run type-check
npm run build
```

缁撴灉锛?

- 鍚庣 `mvn test` 閫氳繃锛? tests, 0 failures, 0 errors銆?
- 鍚庣 `mvn package` 閫氳繃锛岀敓鎴?Spring Boot jar銆?
- 鍓嶅彴 `type-check` 鍜?`build` 閫氳繃銆?
- 鍚庡彴 `type-check` 鍜?`build` 閫氳繃銆?

## 褰撳墠杈圭晫

- Markdown 娓叉煋鐩墠鏄熀纭€瀹炵幇锛屽彧瑕嗙洊鏍囬鍜屾钀斤紱鍚庣画闇€瑕佸寮轰唬鐮佸潡銆佺洰褰曘€丮ermaid銆佸浘鐗囪祫婧愬鐞嗐€?
- 鏂囩珷瀵煎叆銆佸井淇″叕浼楀彿瀵煎嚭銆亃ip 涓嬭浇鎺ュ彛灏氭湭瀹炵幇锛屽綋鍓嶅悗鍙板搴旀寜閽琚偣鍑讳細闇€瑕佸悗缁ˉ榻愩€?
- 鐢ㄦ埛浣撶郴浠嶆槸 bootstrap admin锛屽皻鏈浛鎹㈡垚鏁版嵁搴撶敤鎴峰拰瀵嗙爜鍝堝笇銆?
- 璇勮銆佸弸閾俱€佹枃浠朵笂浼犮€佺珯鐐归厤缃粛灞炰簬涓嬩竴闃舵銆?
- PostgreSQL 鐢熶骇杩炴帴宸查厤缃负鐜鍙橀噺椹卞姩锛屾湰鍦版祴璇曚娇鐢?H2 PostgreSQL mode銆?

## 涓嬩竴闃舵寤鸿

鎺ㄨ崘杩涘叆 Phase 4锛氳瘎璁恒€佸弸閾俱€佹枃浠朵笂浼犮€佺珯鐐归厤缃€?

浼樺厛椤哄簭寤鸿锛?

- `site/settings`锛氳绔欑偣鍚嶇О銆佸ご鍍忋€佺ぞ浜ら摼鎺ャ€丼EO銆佽彍鍗曞彲鍚庡彴閰嶇疆銆?
- `friend`锛氳ˉ榻愬墠鍙板簳閮ㄥ弸閾惧拰鍙嬮摼椤甸潰锛岃В鍐?footer 鏁版嵁鏇村畬鏁寸殑闂銆?
- `comment`锛氭枃绔犺瘎璁烘彁浜ゃ€佸鏍搞€佸睍绀恒€?
- `media`锛氬浘鐗囦笂浼狅紝涓烘枃绔犲皝闈㈠拰姝ｆ枃鍥剧墖鏈嶅姟銆?

杩欐瘮鍏堝仛缁熻/鎼滅储鏇撮€傚悎褰撳墠闃舵锛屽洜涓哄畠鐩存帴琛ラ綈鍗氬缃戠珯鏃ュ父杩愯惀闂幆銆?
