# gitbucket-push-rules-plugin

[English](README.md) | 日本語

リポジトリ毎に設定できるpushルールを提供する [GitBucket](https://github.com/gitbucket/gitbucket) プラグインです。

## 機能

### ブランチpush制限

特定ブランチを更新できるユーザーを制限します。ブランチ名はglobパターンでマッチします:

- `main` — 完全一致
- `release/*` — `release/1.0` に一致(`release/1.0/hotfix` には不一致)
- `**` — すべてのブランチに一致

各ルールには独立した2つのユーザーリストがあります:

- **Push users** — ブランチへ直接pushできるユーザー
- **Merge users** — Web UIからプルリクエストをマージできるユーザー

補足:

- リストが空の場合は「管理者のみ」を意味します。たとえばPush usersを空にして
  Merge usersにリリース担当者を指定すると、「PR経由のみ」の運用を強制できます。
- リポジトリオーナー・グループマネージャー・システム管理者・`ADMIN`ロールの
  コラボレータは常にルールを**バイパス**します(GitBucket標準のProtected Branchと同じポリシー)。
- マージ許可のないユーザーによるWeb UIマージは、現状エラーページとして表示されます
  (GitBucket本体がマージ経路のフックエラーを例外として伝播するため)。
- タグのpushは対象外です。

### 改行コードチェック

指定ディレクトリ配下の指定拡張子ファイルに、期待と異なる改行コード(LF / CRLF)が
持ち込まれるpushを拒否します。

- ルール = ディレクトリプレフィックス(空 = リポジトリ全体)+ カンマ区切りの拡張子(例: `java,xml`)+ 期待する改行コード(`LF` または `CRLF`)
- 検査対象はpushで新しく入るコミットのみです(追加/変更されたファイル。マージコミットは第1親との差分を検査)。
- バイナリファイルと10MB超のファイルはスキップします。
- このルールは内容品質のゲートのため、管理者を含む**全ユーザー**に適用されます。

## インストール

[Releases](https://github.com/takahino/gitbucket-push-rules-plugin/releases) からjarをダウンロードし、
`GITBUCKET_HOME/plugins/`(デフォルト: `~/.gitbucket/plugins/`)に配置してGitBucketを再起動してください。

## 使い方

リポジトリの **Settings → Push Rules**(オーナー/グループマネージャー/管理者のみ表示)を開いて
ルールを追加します。拒否されたpushは次のように表示されます:

```
 ! [remote rejected] main -> main (Push to branch 'main' is restricted by push rules. Contact the repository owner.)
 ! [remote rejected] main -> main (Line ending violation: 'src/A.java' (commit e3286dc) must use LF. (rule: dir='src', extensions='java'))
```

マージ許可のないユーザーによるプルリクエストのマージは次のメッセージで失敗します:

```
Merging into branch 'main' is restricted by push rules. Contact the repository owner.
```

## 対応バージョン

| プラグインバージョン | GitBucketバージョン |
|---|---|
| 1.0.x | 4.46.x |

## ソースからのビルド

```
sbt assembly        # target/scala-2.13/ 配下にプラグインjarをビルド
sbt install         # ビルドして ~/.gitbucket/plugins/ にコピー
sbt test            # ユニットテストを実行
```

## ライセンス

[Apache License 2.0](LICENSE)
