---
title: 自作の JVMライブラリをJitPackで簡単に公開する方法
tags: JitPack.io Java Maven
author: blue_islands
slide: false
---

# はじめに

JVMライブラリの開発者にとって、[JitPack](https://jitpack.io/)はプロジェクトの公開を簡単かつ迅速に行うための重要なツールです。この記事では、Mavenを用いてJitPackでライブラリを効率的に公開する具体的な手順を紹介します。

# JitPackってなに？

[JitPack](https://jitpack.io/)は、GitHubなどのGitリポジトリから直接JVMやAndroidライブラリをビルドし、公開できるサービスです。このサービスを利用することで、ライブラリを世界中の開発者に簡単に提供することが可能となります。

https://jitpack.io/

# JitPackを使ってJVMライブラリを公開してみる

JitPackでライブラリを公開するためには、いくつかの重要な手順を踏む必要があります。

## 手順1: JitPack Mavenリポジトリの追加

プロジェクトの`pom.xml`にJitPackのリポジトリを追加し、JitPackを通じたビルドとアクセスが可能になるように設定します。

```xml:pom.xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

プロジェクトのソースコード管理（SCM）設定も`pom.xml`に追加します。以下は、GitHubリポジトリの仮のURLと接続情報の例です。

```xml:pom.xml
<scm>
    <url>https://github.com/blue-islands/wazahyo</url>
    <connection>scm:git:git@github.com:blue-islands/wazahyo.git</connection>
    <tag>HEAD</tag>
</scm>
```

これらの設定は、JitPackがプロジェクトのソースコードを適切に取得し、ビルドプロセスを実行するために必要です。

## 手順2: プラグインの追加

ビルドとリリースのプロセスを管理するため、`maven-compiler-plugin`と`maven-release-plugin`を`pom.xml`に追加します。これにより、プロジェクトのビルドとリリースがスムーズに行われます。

```xml:pom.xml
<plugin>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <source>17</source>
        <target>17</target>
    </configuration>
</plugin>
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-release-plugin</artifactId>
    <version>3.0.1</version>
    <configuration>
        <!-- リリースバージョンの指定 -->
        <tagNameFormat>v@{project.version}</tagNameFormat>
        <autoVersionSubmodules>true</autoVersionSubmodules>
        <releaseProfiles>release</releaseProfiles>
        <goals>deploy</goals>
        <!-- SCMコメントの設定 -->
        <scmCommentPrefix>[Release]</scmCommentPrefix>
        <!-- スナップショットバージョンの自動インクリメント -->
        <preparationGoals>clean verify</preparationGoals>
    </configuration>
</plugin>
```

## 手順3: インストールの確認

プロジェクトのビルドが正しく行われるかをローカル環境で確認するために、`mvn clean install`コマンドを実行します。これにより、ライブラリのビルドとインストールが正常に完了するかをテストし、動作が正常であることを確認できます。このステップは、公開前の最終確認として非常に重要です。

## 手順4: GitHubでリリース作成

まず、ターミナルまたはコマンドプロンプトで`mvn release:prepare`コマンドを実行し、リリース準備を行います。このコマンドは、バージョン番号の更新、タグの作成、そしてリリース用のコミットを自動的に生成します。

その後、GitHubのリポジトリ画面に移動し、生成されたタグに基づいてリリースを作成します。タイトル、説明を入力し、リリースを公開すると、JitPackはこの新しいリリースを検出し、ライブラリとしてビルドを開始します。GitHubでのリリース作成は、JitPackによるビルドプロセスのトリガーとなり、プロジェクトがJitPack上で利用可能になります。

GitHubを確認してみると、新しいタグが作成されていることが確認できます。

![image.png](https://qiita-image-store.s3.ap-northeast-1.amazonaws.com/0/20464/9c8c362d-3a93-1585-2984-4f401ea9da3a.png)

そのまま、Releases画面を開いて、タイトルと説明を入力して、リリースを公開します。

![image.png](https://qiita-image-store.s3.ap-northeast-1.amazonaws.com/0/20464/d40e28ed-c99e-6149-7e39-5f1251281b09.png)

リリースの作成が完了したら、JitPackへアクセスして、リポジトリのURLを入力して検索を行います。

![image.png](https://qiita-image-store.s3.ap-northeast-1.amazonaws.com/0/20464/3c39c999-6fb1-83bc-448d-fa453b4b6c6c.png)

ビルドが成功すると、Logのアイコンが緑色になります。

![image.png](https://qiita-image-store.s3.ap-northeast-1.amazonaws.com/0/20464/6e384da2-7ad1-c153-c459-18ad60c8867f.png)

# 公開されたライブラリの使用方法

公開されたライブラリは、他のプロジェクトの`pom.xml`に依存関係として追加することで利用可能になります。これにより、他の開発者が簡単にライブラリをプロジェクトに組み込めるようになります。

```xml:pom.xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
<dependency>
    <groupId>com.github.blue-islands</groupId>
    <artifactId>wazahyo</artifactId>
    <version>VERSION</version>
</dependency>
```

# おまけ

公開プロセス中に遭遇する可能性のある問題や、それらの対処方法についても触れます。これには、ビルドエラーの解決や、JitPackとの連携に関する一般的なヒントが含まれます。

## その1: SNAPSHOTの付け忘れ

```text
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-release-plugin:3.0.1:prepare (default-cli) on project noby-java-client: You don't have a SNAPSHOT project in the reactor projects list. -> [Help 1]
```

エラー解決のための簡潔な手順は次の通りです。Mavenの`release:prepare`コマンドのエラーは、通常、プロジェクトがSNAPSHOTバージョンでない場合に発生します。`pom.xml`でバージョンがSNAPSHOTであることを確認し、必要に応じてSNAPSHOTバージョンに更新します。変更をコミットし、プッシュした後、再度コマンドを実行してみてください。解決しない場合は、Mavenのデバッグオプションを使用して詳細情報を取得し、公式ドキュメントやフォーラムを参照してください。

## その2: コミット忘れ

```text
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-release-plugin:3.0.1:prepare (default-cli) on project jsonrpc2client: Cannot prepare the release because you have local modifications :
[ERROR] [pom.xml:modified]
[ERROR]
[ERROR] -> [Help 1]
```

このエラーは、Mavenの`release:prepare`コマンド実行時にローカルで未コミットの変更があることを示しています。対処法としては、まず`git status`で変更されたファイルを確認し、意図した変更であれば`git add`と`git commit`コマンドで変更をコミットします。その後、再度`mvn release:prepare`を実行することで、リリースプロセスを進めることができます。未コミットの変更がある場合、Mavenのリリースプロセスは正常に進行しませんので、変更の管理とコミットが重要です。


# このリポジトリへの適用内容（wazahyo）

このリポジトリでは、上記手順を反映して `pom.xml` に以下を適用しています。

- `version` を `1.0.0-SNAPSHOT` に変更（`mvn release:prepare` 実行要件）
- `scm` セクションを `blue-islands/wazahyo` 向けに追加
- `maven-compiler-plugin`（Java 17）を追加
- `maven-release-plugin` を追加

実際のリリース時は、`mvn release:prepare` 実行後に GitHub Releases でタグに対応するリリースを公開してください。


# まとめ

JitPackを利用すれば、JVMライブラリの公開がより簡単かつ効率的になります。このガイドが、ライブラリ公開のプロセスをスムーズに進める手助けとなることを願います。

# 参考にした記事

https://qiita.com/takagimeow/items/a62ba44c11117740c018
