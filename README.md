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
java -jar target/wazahyo-1.0.0-SNAPSHOT.jar help
```

## 使い方（CLI）

```bash
# 緯度経度 -> 和座標コード
java -jar target/wazahyo-1.0.0-SNAPSHOT.jar encode-latlon 35.681236 139.767125

# 和座標コード -> 9次メッシュコード
java -jar target/wazahyo-1.0.0-SNAPSHOT.jar decode-meshcode せ春里か湖

# 和座標コード -> 緯度経度（9次メッシュ中心）
java -jar target/wazahyo-1.0.0-SNAPSHOT.jar decode-latlon せ春里か湖
```

## ドキュメント

- 設計書: [`docs/design.md`](docs/design.md)
- JitPack公開ガイド: [`docs/jitpack-publish-guide.md`](docs/jitpack-publish-guide.md)

## JitPackでの利用方法

[![](https://jitpack.io/v/livlog-llc/wazahyo.svg)](https://jitpack.io/#livlog-llc/wazahyo)

Gitプロジェクトとして公開している `livlog-llc/wazahyo` は、JitPack経由で依存関係として利用できます。

### Step 1. リポジトリを追加（Maven）

`pom.xml` に JitPack リポジトリを追加します。

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

### Step 2. 依存関係を追加

```xml
<dependency>
    <groupId>com.github.livlog-llc</groupId>
    <artifactId>wazahyo</artifactId>
    <version>v1.0.0</version>
</dependency>
```

### Step 3. ソースコードから呼び出す

`WazahyoCodec` はすべて `static` メソッドなので、そのまま呼び出して利用できます。

```java
import llc.livlog.wazahyo.WazahyoCodec;

public class WazahyoExample {
    public static void main(String[] args) {
        double latitude = 35.681236;
        double longitude = 139.767125;

        // 緯度経度 -> 和座標コード
        String code = WazahyoCodec.encodeFromLatLon(latitude, longitude);

        // 和座標コード -> 9次メッシュコード
        String meshCode = WazahyoCodec.decodeToMeshCode(code);

        // 和座標コード -> 緯度経度（9次メッシュ中心）
        WazahyoCodec.LatLon center = WazahyoCodec.decodeToLatLon(code);

        System.out.println("wazahyo code: " + code);
        System.out.println("mesh code   : " + meshCode);
        System.out.printf("center lat/lon: %.8f, %.8f%n", center.latitude(), center.longitude());
    }
}
```

APIの詳しい変換フロー例は `src/main/java/llc/livlog/wazahyo/IdeRunSample.java` も参照してください。

初回リクエスト時にJitPackがコードをチェックアウトしてビルドし、生成物（jar / aar）を配信します。
GitHub Releases がない場合は、短いコミットハッシュや `master-SNAPSHOT` をバージョンとして指定できます。

## コントリビュート

和座標を広く使えるOSSに育てるため、Issue・Pull Requestを歓迎します。

- バグ報告
- 仕様改善の提案
- 実装改善（性能・可読性・テスト）
- 他言語実装やツール連携の提案

小さな改善でも大歓迎です。

## ライセンス

本プロジェクトは [MIT License](LICENSE) のもとで公開しています。
