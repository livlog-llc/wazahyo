# ソースコードレビュー（2026-03-10）

対象: `src/main/java/llc/livlog/wazahyo`

## 総評

実装は全体として読みやすく、`WazahyoCodec` に責務が集約されており API も明快です。
一方で、**API の一貫性** と **CLI ドキュメントの保守性** に改善余地があります。

## 指摘事項

### 1) `encode` と `decodeToMeshCode` の対応範囲が一致しない（重要度: Medium）

- `encode(int mesh1Group, int[] parts)` は `mesh1Group` として `0..199` を受け付けます。
- しかし `decodeToMeshCode(String)` は、復号後の `mesh1Group` が `SUPPORTED_MESH1_CODES`（175件）外だと失敗します。
- そのため、`encode` で生成できるコードの一部は `decodeToMeshCode` で逆変換できません。

**影響**

- API 利用者が「encode できたなら mesh code へも戻せる」と想定するとランタイム例外になります。

**提案**

- 仕様として許容するなら README/JavaDoc で明記する。
- もしくは API でモードを分離（例: `encodeSupportedMeshOnly` など）して期待値を明確化する。

### 2) CLI ヘルプの JAR 名が固定値で保守コストが高い（重要度: Low）

- `App#usage` が `wazahyo-1.0.0.jar` を固定表示しており、実際の配布名（例: `-SNAPSHOT`）とズレる可能性があります。

**提案**

- バージョン固定をやめ、`target/wazahyo-<version>.jar` のような表記に変更する。

### 3) サンプルコードのコメントと座標が不一致（重要度: Low）

- `IdeRunSample` のコメントが「東京駅付近」ですが、値は富士山周辺の座標です。

**影響**

- 初見ユーザーがデータの意味を誤解する可能性があります。

**提案**

- コメントを実データに合わせる。

## 今回反映した改善

- 指摘 2: CLI ヘルプの JAR 名表記をバージョン非依存に変更。
- 指摘 3: サンプルコメントを座標に合わせて修正。

