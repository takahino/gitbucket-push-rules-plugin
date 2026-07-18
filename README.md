# gitbucket-push-rules-plugin

A [GitBucket](https://github.com/gitbucket/gitbucket) plugin that provides configurable push rules per repository.

## Features

### Branch push restriction

Allow only specific users to push to specific branches. Branch names are matched by glob patterns:

- `main` — exact match
- `release/*` — matches `release/1.0` but not `release/1.0/hotfix`
- `**` — matches all branches

Notes:

- Repository owners, group managers, system administrators and `ADMIN`-role collaborators always **bypass** these rules (same policy as GitBucket's built-in Protected Branch).
- Rules are also enforced when a pull request is merged from the Web UI, so the restriction cannot be bypassed by opening a PR and merging it. (The merge fails with an error in that case.)
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
