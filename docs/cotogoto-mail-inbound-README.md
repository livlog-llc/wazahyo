# cotogoto-mail-inbound README

`cotogoto-mail-inbound` は、受信メールをアプリケーションに取り込み、
後続の業務処理（通知・自動返信・チケット化など）へ安全に接続するためのインバウンド基盤です。

> この README は、実装・運用の起点として使えるように、目的・構成・API・運用手順をひとまとめにしています。

## 1. 目的

- メール受信を単一の入口に集約する
- 送信元検証・重複排除・監査ログを標準化する
- 本文／添付ファイルを構造化して下流システムへ連携する
- 障害時に再処理可能な設計にする

## 2. 想定ユースケース

- 問い合わせメールをチケットシステムへ自動登録
- 特定アドレス宛メールを社内ワークフローに振り分け
- 添付ファイル（PDF/CSV）をバッチ連携キューへ投入
- メール本文のキーワード判定による通知・アラート

## 3. 全体構成（論理）

1. **Inbound Provider**（SES / SendGrid / Mailgun など）
2. **Webhook Endpoint**（`cotogoto-mail-inbound`）
3. **Validation Layer**（署名検証・許可ドメイン判定）
4. **Normalizer**（ヘッダ・本文・添付ファイルの標準化）
5. **Dispatcher**（DB保存 / Queue投入 / 外部API通知）
6. **Observability**（ログ・メトリクス・アラート）

## 4. 受信データモデル（例）

```json
{
  "messageId": "provider-message-id",
  "receivedAt": "2026-03-11T00:00:00Z",
  "from": "sender@example.com",
  "to": ["support@cotogoto.example"],
  "subject": "お問い合わせ",
  "textBody": "プレーンテキスト本文",
  "htmlBody": "<p>HTML本文</p>",
  "attachments": [
    {
      "fileName": "sample.pdf",
      "contentType": "application/pdf",
      "size": 12345,
      "storageKey": "inbound/2026/03/11/sample.pdf"
    }
  ],
  "headers": {
    "Message-Id": "<...>",
    "In-Reply-To": "<...>"
  }
}
```

## 5. API 仕様（例）

### `POST /webhooks/mail/inbound`

メールプロバイダからの受信通知を受け付けます。

#### リクエストヘッダ（例）

- `Content-Type: application/json`
- `X-Signature: <provider-signature>`
- `X-Request-Id: <optional-trace-id>`

#### レスポンス

- `200 OK`: 正常受付
- `202 Accepted`: 非同期処理キューへの投入完了
- `400 Bad Request`: 形式不正
- `401/403`: 署名検証失敗
- `409 Conflict`: 重複メッセージ
- `500`: 内部エラー

## 6. セキュリティ要件

- Webhook 署名検証を必須化
- 許可IP/許可ドメイン制御（可能な範囲で）
- 添付ファイルサイズ上限、拡張子/Content-Type制限
- PII を含むログのマスキング
- Secrets は環境変数または Secret Manager で管理

## 7. 重複排除と再処理

- `messageId` をキーに冪等処理
- 受信原文を一定期間保存（例: 30日）
- 失敗レコードは Dead Letter Queue へ退避
- 管理者向けに手動再処理手順を用意

## 8. 設定項目（例）

| 変数名 | 必須 | 説明 |
|---|---|---|
| `MAIL_INBOUND_PROVIDER` | 必須 | 利用するプロバイダ名 |
| `MAIL_INBOUND_SIGNING_SECRET` | 必須 | 署名検証用シークレット |
| `MAIL_INBOUND_MAX_ATTACHMENT_MB` | 任意 | 添付ファイルの最大サイズ |
| `MAIL_INBOUND_ALLOWED_DOMAINS` | 任意 | 許可送信元ドメイン（CSV） |
| `MAIL_INBOUND_DLQ_ENABLED` | 任意 | DLQ利用フラグ |

## 9. ローカル開発手順（雛形）

```bash
# 1) 依存関係の解決
# 2) 環境変数の設定
# 3) アプリ起動
# 4) ngrok 等で webhook を公開
# 5) テストイベントを送信
```

> 実装言語やフレームワークに合わせて、上記を具体コマンドに置き換えてください。

## 10. 監視・運用

最低限の可観測性として以下を推奨します。

- 受信件数 / 失敗件数 / 重複件数
- 署名検証失敗率
- 添付ファイル処理エラー率
- 処理遅延（受信から下流投入までの時間）

### アラート例

- 5分間で `500` が連続閾値超過
- 署名検証失敗率が急増
- DLQ蓄積件数がしきい値超過

## 11. トラブルシューティング

- **401/403 が増加**: シークレット不一致、署名対象文字列差分を確認
- **409 が多い**: プロバイダ再送設定と冪等キー実装を確認
- **添付で失敗**: サイズ制限・MIMEタイプ・ストレージ権限を確認
- **文字化け**: `charset` とデコード処理（UTF-8 / ISO-2022-JP）を確認

## 12. 今後の拡張候補

- スレッド復元（`In-Reply-To` / `References` 活用）
- 迷惑メールスコア連携
- LLM を使った本文分類・要約
- 管理画面からの再処理・検索

---

必要に応じて、この README を以下の2ファイルに分割してください。

- `README.md`: 概要・起動方法・最小運用
- `docs/architecture.md`: 詳細設計・運用Runbook
