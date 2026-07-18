# gitbucket-push-rules-plugin

A [GitBucket](https://github.com/gitbucket/gitbucket) plugin that provides configurable push rules per repository.

## Features

- **Branch push restriction** — Allow only specific users to push to specific branches (glob patterns like `main`, `release/*`).
- **Line ending check** — Reject pushes that introduce unexpected line endings (LF/CRLF) to files with specific extensions under a specific directory.

## Installation

Download the jar from [Releases](https://github.com/takahino/gitbucket-push-rules-plugin/releases) and put it into `~/.gitbucket/plugins/`, then restart GitBucket.

## Usage

Open **Settings → Push Rules** on a repository (owner / group manager / administrator only) and add rules.

## Compatibility

| Plugin version | GitBucket version |
|---|---|
| 1.0.x | 4.46.x |

## Build

```
sbt assembly
```

## License

Apache License 2.0
