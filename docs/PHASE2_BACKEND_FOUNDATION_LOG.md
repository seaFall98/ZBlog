# PHASE2_BACKEND_FOUNDATION_LOG.md

> 鏈枃鑱岃矗锛氳褰?Phase 2 鍚庣鍩虹宸ョ▼鐨勫疄闄呰惤鍦扮粨鏋溿€侀獙璇佸懡浠ゅ拰涓嬩竴姝ヨ鎺ョ偣銆?

## 鏈钀藉湴

鏃堕棿锛?026-05-15 鏈湴鎵ц璁板綍銆?

宸插畬鎴愶細

- 鍒涘缓 `server/` Maven 宸ョ▼锛屽熀浜?Java 21 + Spring Boot 3.3.5銆?
- 寤虹珛缁熶竴鍝嶅簲妯″瀷 `ApiResponse`锛岀害瀹氭垚鍔熷搷搴?`code=OK`銆乣message=success`銆?
- 寤虹珛缁熶竴寮傚父澶勭悊鍏ュ彛锛岃鐩栦笟鍔″紓甯搞€佸弬鏁版牎楠屽紓甯稿拰鏈鏈熷紓甯搞€?
- 寤虹珛 JWT 鐧诲綍楠ㄦ灦锛屽綋鍓嶄娇鐢?bootstrap admin 閰嶇疆浣滀负涓存椂鍚庡彴璐﹀彿鏉ユ簮銆?
- 寤虹珛鏃犵姸鎬?Spring Security 閰嶇疆锛屽紑鏀惧仴搴锋鏌ャ€佺櫥褰曘€丱penAPI/Swagger 鍏ュ彛锛屼繚鎶ゅ悗鍙?API銆?
- 寤虹珛 `/api/v1/health`銆乣/api/v1/auth/login`銆乣/api/v1/admin/ping` 涓変釜鍩虹鎺ュ彛銆?
- 鍔犲叆 OpenAPI銆丄ctuator銆丗lyway銆丳ostgreSQL銆丣WT 绛?Phase 2 渚濊禆鍩虹嚎銆?

## 楠岃瘉缁撴灉

宸叉墽琛岋細

```powershell
cd D:\MyCode\ZBlogProject\ZBlog\server
mvn test
mvn package
```

缁撴灉锛?

- `mvn test` 閫氳繃锛? 涓祴璇曪紝0 failure锛? error銆?
- `mvn package` 閫氳繃锛岀敓鎴?`server\target\zblog-server-0.1.0-SNAPSHOT.jar`銆?

## 褰撳墠杈圭晫

- 杩欎竴姝ヨ繕娌℃湁鎺ュ叆鐪熷疄鏁版嵁搴撹〃銆丷edis銆丒S 鎴栨枃浠跺瓨鍌ㄣ€?
- bootstrap admin 鍙槸寮€鍙戦樁娈典复鏃剁櫥褰曞叆鍙ｏ紝鍚庣画浼氭浛鎹负鏁版嵁搴撶敤鎴峰拰瀵嗙爜鍝堝笇銆?
- FlecBlog 鍓嶅彴鐜板湪鑿滃崟鍜屽簳閮ㄤ负绌烘槸姝ｅ父鐜拌薄锛屽洜涓鸿繖浜涙暟鎹渶瑕佸悗缁?Java API 鎻愪緵銆?
- PostgreSQL/Flyway 渚濊禆宸茶繘鍏ュ伐绋嬪熀绾匡紝浣嗗疄闄?schema 鍜?datasource 浼氬湪鏂囩珷绯荤粺闂幆闃舵钀藉湴锛岄伩鍏?Phase 2 杩囨棭寮曞叆杩愯渚濊禆闃诲銆?

## 涓嬩竴姝?

Phase 3 寤鸿浼樺厛琛ラ綈 FlecBlog 鍓嶇鏈€渚濊禆鐨勬暟鎹棴鐜細

- `site`锛氱珯鐐归厤缃€佽彍鍗曘€侀〉鑴氬鑸€佸弸閾惧熀纭€鏁版嵁銆?
- `content`锛氭枃绔犲垪琛ㄣ€佹枃绔犺鎯呫€佸垎绫汇€佹爣绛俱€?
- `identity`锛氭妸涓存椂 bootstrap admin 鏇挎崲涓烘暟鎹簱璐﹀彿銆?
- `migration`锛氬缓绔?PostgreSQL schema 鍜?Flyway baseline銆?
