# IMPLEMENTATION_ROADMAP.md

> 鏈枃鑱岃矗锛氭妸 ZBlog 浠庡綋鍓嶇┖椤圭洰鎺ㄨ繘鍒板彲涓婄嚎鍗氬缃戠珯鐨勯樁娈佃鍒掋€佷骇鐗╁拰楠屾敹鏍囧噯鍐欐竻妤氥€?

## Phase 0: 鏂囨。涓庡弬鑰冩簮鍑嗗

鐩爣锛?

- 鍒濆鍖?ZBlog锛?
- 鍏嬮殕 FlecBlog 鍒?`_reference`锛?
- 瀹屾垚鏂囨。鍖咃紱
- 鏄庣‘ FlecBlog 鍓嶇浼樺厛澶嶇敤鍜屾棫 ZBlog 璧勪骇杩佺Щ杈圭晫銆?

楠屾敹锛?

```powershell
git rev-parse --is-inside-work-tree
Test-Path _reference\FlecBlog
Test-Path docs
Select-String -Path docs\*.md -Pattern "鏈枃鑱岃矗"
```

## Phase 1: 鍓嶇婧愮爜杩佸叆

鐩爣锛?

- 灏?FlecBlog `blog` 杩佸叆 `blog/`锛?
- 灏?FlecBlog `admin` 杩佸叆 `admin/`锛?
- 淇濈暀 attribution锛?
- 瀹屾垚鏈湴瀹夎鍜屾瀯寤猴紱
- 鍙仛蹇呰鍝佺墝鍜屽叆鍙ｆ竻鐞嗭紝涓嶉噸璁捐銆?

楠屾敹锛?

```powershell
cd blog
npm install
npm run type-check
npm run build

cd ..\admin
npm install
npm run type-check
npm run build
```

## Phase 2: Java 鍚庣鍩虹宸ョ▼

鐩爣锛?

- 鍒涘缓 Spring Boot 3 + Java 21 鍚庣锛?
- 鍔犲叆缁熶竴鍝嶅簲銆佸紓甯搞€佹牎楠屻€丱penAPI锛?
- PostgreSQL + Flyway锛?
- 鐧诲綍璁よ瘉鍜?JWT锛?
- 鍋ュ悍妫€鏌ャ€?

楠屾敹锛?

```powershell
cd server
mvn test
mvn package
```

2026-05-15 鎵ц缁撴灉锛?
- `server/` Spring Boot 鍚庣鍩虹宸ョ▼宸插垱寤恒€?
- 宸茶惤鍦扮粺涓€鍝嶅簲銆佺粺涓€寮傚父銆佸弬鏁版牎楠屻€丣WT 鐧诲綍銆佹棤鐘舵€佸畨鍏ㄩ厤缃€佸仴搴锋鏌ャ€丱penAPI 鍏ュ彛銆?
- `mvn test` 閫氳繃锛? tests, 0 failures, 0 errors銆?
- `mvn package` 閫氳繃锛氱敓鎴?Spring Boot 鍙繍琛?jar銆?
- 璇︾粏璁板綍瑙?`docs/PHASE2_BACKEND_FOUNDATION_LOG.md`銆?

## Phase 3: 鏂囩珷绯荤粺闂幆

鐩爣锛?

- 鏂囩珷銆佸垎绫汇€佹爣绛炬暟鎹ā鍨嬶紱
- Markdown 鐪熸簮銆丠TML 蹇収銆乀OC锛?
- 鍚庡彴鏂囩珷 CRUD锛?
- 鍓嶅彴鏂囩珷鍒楄〃鍜岃鎯?API銆?

楠屾敹锛?

- 鍚庡彴鍙繚瀛樿崏绋垮拰鍙戝竷鏂囩珷锛?
- 鍓嶅彴鍙鍙栨枃绔犲垪琛ㄥ拰璇︽儏锛?
- Markdown銆佷唬鐮佸潡銆佺洰褰曟甯搞€?

2026-05-15 鎵ц缁撴灉锛?
- `server/` 宸茶惤鍦版枃绔犮€佸垎绫汇€佹爣绛俱€佽彍鍗曟暟鎹簱 baseline銆?
- 宸插疄鐜板叕寮€鏂囩珷璇诲彇闂幆锛氳彍鍗曘€佸垎绫汇€佹爣绛俱€佹枃绔犲垪琛ㄣ€佹枃绔犺鎯呫€侀殢鏈烘枃绔犮€?
- 宸插疄鐜板悗鍙版枃绔犮€佸垎绫汇€佹爣绛?CRUD 鍜屾枃绔犲彂甯?鍙栨秷鍙戝竷銆?
- 宸叉帴鍏?Flyway + JDBC + PostgreSQL 鐢熶骇閰嶇疆锛屾祴璇曚娇鐢?H2 PostgreSQL mode銆?
- 宸茶皟鏁寸粺涓€鍝嶅簲涓哄墠绔吋瀹圭殑 `code=0` 鏁板€肩爜銆?
- `mvn test` / `mvn package` / 鍓嶅悗鍙?`type-check` / 鍓嶅悗鍙?`build` 鍧囬€氳繃銆?
- 璇︾粏璁板綍瑙?`docs/PHASE3_CONTENT_CLOSED_LOOP_LOG.md`銆?

## Phase 4: 璇勮銆佸弸閾俱€佹枃浠躲€佽缃?

鐩爣锛?

- 璇勮鎻愪氦銆佸鏍搞€佸睍绀猴紱
- 鍙嬮摼灞曠ず鍜岀鐞嗭紱
- 鏂囦欢/鍥剧墖涓婁紶锛?
- 绔欑偣閰嶇疆锛?
- 鍚庡彴瀵瑰簲椤甸潰鎺ュ叆 Java API銆?

楠屾敹锛?

- 鍚庡彴鍙畬鎴愭棩甯稿崥瀹㈢鐞嗭紱
- 鍓嶅彴璇勮銆佸弸閾俱€佺珯鐐逛俊鎭彲鐢ㄣ€?

鎵ц缁撴灉锛?

- 宸插畬鎴愯瘎璁恒€佸弸閾俱€佹枃浠朵笂浼犮€佺珯鐐硅缃殑鍚庣闂幆銆?
- 鏂板 Phase 4 闆嗘垚娴嬭瘯锛岃鐩栬缃鍙?鏇存柊銆佸弸閾剧鐞?鐢宠銆佽瘎璁烘彁浜?瀹℃牳/鍒犻櫎銆佹枃浠朵笂浼?鍒楄〃/鍒犻櫎銆?
- `mvn test` / `mvn package` / 鍓嶅悗鍙?`type-check` / 鍓嶅悗鍙?`build` 鍧囬€氳繃銆?
- 璇︾粏璁板綍瑙?`docs/PHASE4_INTERACTION_SITE_MEDIA_LOG.md`銆?

## Phase 5: 缁熻銆丼EO銆佹悳绱?

鐩爣锛?

- 璁块棶缁熻鍜屼华琛ㄧ洏锛?
- sitemap銆乫eed銆丱pen Graph锛?
- 鎼滅储鍔熻兘锛?
- Redis 缂撳瓨闃呰閲忓拰鐑棬鏂囩珷銆?

楠屾敹锛?

- sitemap/feed 鍙闂紱
- 鎼滅储鍙敤锛?
- 鐑棬/鎺ㄨ崘鏂囩珷鏁版嵁绋冲畾銆?

## Phase 6: 閮ㄧ讲涓婄嚎

鐩爣锛?

- Dockerfile锛?
- Docker Compose锛?
- Nginx锛?
- HTTPS锛?
- 鏁版嵁澶囦唤锛?
- README 閮ㄧ讲璇存槑锛?
- 鍙戝竷 checklist銆?

楠屾敹锛?

```powershell
docker compose config
```

骞跺畬鎴愪簯鏈嶅姟鍣ㄩ儴缃?smoke test銆?

## Phase 7: 浣撻獙鎵撶（

鐩爣锛?

- 鍓嶅彴闃呰浣撻獙锛?
- 绉诲姩绔竷灞€锛?
- 鍚庡彴琛ㄥ崟浣撻獙锛?
- loading/empty/error锛?
- 瑙嗚缁嗚妭銆?

楠屾敹锛?

- 涓昏椤甸潰鎴浘楠屾敹锛?
- 绉诲姩绔笉宕╋紱
- 椤甸潰涓嶇矖绯欍€佷笉鍍忛粯璁ゆā鏉裤€?
