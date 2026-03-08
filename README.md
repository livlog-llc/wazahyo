# 和座標（Wazahyo）

和座標は、日本の位置情報を「5文字の和文字コード」で表現するオープンソースプロジェクトです。

このリポジトリでは、和座標のJava実装（エンコード/デコード）を公開し、誰でも試せる・組み込める形で提供しています。

## このプロジェクトが目指すこと

- **誰でも使える位置表現**
  - 地図アプリ、創作、教育、地域活動など、用途を限定せず使える共通基盤にする
- **オープンな仕様と実装**
  - 実装だけでなく仕様も公開し、コミュニティで改善できるようにする
- **文化と技術の接続**
  - 和文字コードを通じて、位置情報に日本語的な表現の余地を持たせる

## クイックスタート

### 要件

- Java 17+
- Maven 3.9+

### ビルド

```bash
mvn clean package
```

生成されたJARでCLIを実行できます。

```bash
java -jar target/wazahyo-1.0.0.jar help
```

## 使い方（CLI）

```bash
# 緯度経度 -> 和座標コード
java -jar target/wazahyo-1.0.0.jar encode-latlon 35.681236 139.767125

# 和座標コード -> 9次メッシュコード
java -jar target/wazahyo-1.0.0.jar decode-meshcode せ春里か湖

# 和座標コード -> 緯度経度（9次メッシュ中心）
java -jar target/wazahyo-1.0.0.jar decode-latlon せ春里か湖
```

## ドキュメント

- 設計書: [`docs/design.md`](docs/design.md)
- JitPack公開ガイド: [`docs/jitpack-publish-guide.md`](docs/jitpack-publish-guide.md)

## コントリビュート

和座標を広く使えるOSSに育てるため、Issue・Pull Requestを歓迎します。

- バグ報告
- 仕様改善の提案
- 実装改善（性能・可読性・テスト）
- 他言語実装やツール連携の提案

小さな改善でも大歓迎です。

## ライセンス

本プロジェクトは [MIT License](LICENSE) のもとで公開しています。
