# gitbucket-push-rules-plugin

English | [日本語](README.ja.md)

A [GitBucket](https://github.com/gitbucket/gitbucket) plugin that provides configurable push rules per repository.

## Features

### Branch push restriction

Restrict who can update specific branches. Branch names are matched by glob patterns:

- `main` — exact match
- `release/*` — matches `release/1.0` but not `release/1.0/hotfix`
- `**` — matches all branches

Each rule has two independent user lists:

- **Push users** — users who can push directly to the branch.
- **Merge users** — users who can merge pull requests into the branch from the Web UI.

Notes:

- An empty list means "administrators only". For example, leaving push users empty and setting
  merge users to your release managers enforces a PR-only workflow.
- Repository owners, group managers, system administrators and `ADMIN`-role collaborators always
  **bypass** these rules (same policy as GitBucket's built-in Protected Branch).
- A rejected Web UI merge currently surfaces as an error page (GitBucket propagates hook errors
  of the merge path as an exception).
- Tag pushes are not affected.

### Line ending check

Reject pushes that introduce unexpected line endings (LF / CRLF) to files with the specified
extensions under the specified directory.

- Rule = directory prefix (empty = whole repository) + comma-separated extensions (e.g. `java,xml`) + expected EOL (`LF` or `CRLF`).
- Only commits newly introduced by the push are scanned (added/modified files; merges are checked against their first parent).
- Binary files and files larger than 10 MB are skipped.
- These rules apply to **all** users, including administrators, because they are a content quality gate.

## Installation

Download the jar from [Releases](https://github.com/takahino/gitbucket-push-rules-plugin/releases),
put it into `GITBUCKET_HOME/plugins/` (default: `~/.gitbucket/plugins/`), and restart GitBucket.

## Usage

Open **Settings → Push Rules** on a repository (visible to owners, group managers and administrators)
and add rules. A rejected push looks like this:

```
 ! [remote rejected] main -> main (Push to branch 'main' is restricted by push rules. Contact the repository owner.)
 ! [remote rejected] main -> main (Line ending violation: 'src/A.java' (commit e3286dc) must use LF. (rule: dir='src', extensions='java'))
```

A pull request merge by a user who is not a merge user fails with:

```
Merging into branch 'main' is restricted by push rules. Contact the repository owner.
```

## Compatibility

| Plugin version | GitBucket version |
|---|---|
| 1.0.x | 4.46.x |

## Build from source

```
sbt assembly        # build the plugin jar under target/scala-2.13/
sbt install         # build and copy into ~/.gitbucket/plugins/
sbt test            # run unit tests
```

## License

[Apache License 2.0](LICENSE)
