package io.github.takahino.gitbucket.pushrules.hook

import gitbucket.core.model.Profile._
import gitbucket.core.plugin.ReceiveHook
import io.github.takahino.gitbucket.pushrules.model.LineEndingRule
import io.github.takahino.gitbucket.pushrules.service.PushRuleService
import org.eclipse.jgit.diff.{DiffEntry, DiffFormatter, RawText}
import org.eclipse.jgit.errors.{IncorrectObjectTypeException, LargeObjectException, MissingObjectException}
import org.eclipse.jgit.lib.{ObjectId, Repository}
import org.eclipse.jgit.revwalk.{RevCommit, RevWalk}
import org.eclipse.jgit.transport.{ReceiveCommand, ReceivePack}
import org.eclipse.jgit.treewalk.{CanonicalTreeParser, EmptyTreeIterator}
import org.eclipse.jgit.util.io.DisabledOutputStream
import profile.api._

import scala.jdk.CollectionConverters._
import scala.util.Using

/**
 * 改行コードチェックフック。
 * pushで新しく入るコミットの追加/変更ファイルのうち、ルールの対象
 * (指定ディレクトリ配下かつ指定拡張子)のものに期待と異なる改行コードが
 * 含まれていたらpushを拒否する。内容品質のゲートなので管理者もバイパスしない。
 */
class LineEndingRuleHook extends ReceiveHook with PushRuleService {

  // 巨大blobでのOOMを避けるため、これを超えるファイルは検査対象外とする
  private val MaxBlobSize = 10 * 1024 * 1024

  override def preReceive(
    owner: String,
    repository: String,
    receivePack: ReceivePack,
    command: ReceiveCommand,
    pusher: String,
    mergePullRequest: Boolean
  )(implicit session: Session): Option[String] = {
    if (!command.getRefName.startsWith("refs/heads/") || command.getType == ReceiveCommand.Type.DELETE) {
      None
    } else {
      val rules = getEnabledLineEndingRules(owner, repository)
      if (rules.isEmpty) None else checkNewCommits(receivePack.getRepository, command, rules)
    }
  }

  private def checkNewCommits(
    repo: Repository,
    command: ReceiveCommand,
    rules: List[LineEndingRule]
  ): Option[String] = {
    Using.resource(new RevWalk(repo)) { revWalk =>
      revWalk.markStart(revWalk.parseCommit(command.getNewId))
      // 既存refから到達可能なコミットを除外し、今回のpushで新しく入るコミットだけを検査する
      repo.getRefDatabase.getRefs.asScala.foreach { ref =>
        markUninterestingQuietly(revWalk, ref.getObjectId)
      }
      if (command.getOldId != null && !ObjectId.zeroId.equals(command.getOldId)) {
        markUninterestingQuietly(revWalk, command.getOldId)
      }
      revWalk.iterator().asScala
        .map(commit => checkCommit(repo, revWalk, commit, rules))
        .collectFirst { case Some(error) => error }
    }
  }

  private def markUninterestingQuietly(revWalk: RevWalk, id: ObjectId): Unit = {
    try {
      revWalk.markUninteresting(revWalk.parseCommit(id))
    } catch {
      // タグ等コミット以外を指すref、または未取得オブジェクトは無視してよい
      case _: IncorrectObjectTypeException | _: MissingObjectException => ()
    }
  }

  private def checkCommit(
    repo: Repository,
    revWalk: RevWalk,
    commit: RevCommit,
    rules: List[LineEndingRule]
  ): Option[String] = {
    Using.resource(new DiffFormatter(DisabledOutputStream.INSTANCE)) { diffFormatter =>
      diffFormatter.setRepository(repo)
      val entries = if (commit.getParentCount == 0) {
        Using.resource(repo.newObjectReader()) { reader =>
          diffFormatter.scan(new EmptyTreeIterator(), new CanonicalTreeParser(null, reader, commit.getTree))
        }
      } else {
        // マージコミットは第1親との差分(=取り込まれる変更)を検査する
        val parent = revWalk.parseCommit(commit.getParent(0).getId)
        diffFormatter.scan(parent.getTree, commit.getTree)
      }
      entries.asScala.iterator
        .filter(_.getChangeType != DiffEntry.ChangeType.DELETE)
        .flatMap { entry =>
          val path = entry.getNewPath
          rules
            .find(rule => ruleMatches(rule, path))
            .flatMap(rule => checkBlob(repo, entry, path, rule, commit))
        }
        .nextOption()
    }
  }

  private[hook] def ruleMatches(rule: LineEndingRule, path: String): Boolean = {
    val prefix = rule.dirPrefix.replace('\\', '/').stripPrefix("/").stripSuffix("/")
    val inDir = prefix.isEmpty || path.startsWith(prefix + "/")
    val fileName = path.substring(path.lastIndexOf('/') + 1)
    val dotIndex = fileName.lastIndexOf('.')
    val ext = if (dotIndex < 0) "" else fileName.substring(dotIndex + 1).toLowerCase
    val ruleExts = rule.extensions.split(",").map(_.trim.stripPrefix(".").toLowerCase).filter(_.nonEmpty)
    inDir && ruleExts.contains(ext)
  }

  private def checkBlob(
    repo: Repository,
    entry: DiffEntry,
    path: String,
    rule: LineEndingRule,
    commit: RevCommit
  ): Option[String] = {
    try {
      val bytes = repo.open(entry.getNewId.toObjectId).getCachedBytes(MaxBlobSize)
      if (RawText.isBinary(bytes)) {
        None
      } else if (hasViolation(bytes, rule.expectedEol)) {
        Some(
          s"Line ending violation: '$path' (commit ${commit.getName.take(7)}) must use ${rule.expectedEol}. " +
            s"(rule: dir='${rule.dirPrefix}', extensions='${rule.extensions}')"
        )
      } else {
        None
      }
    } catch {
      case _: LargeObjectException => None
    }
  }

  private[hook] def hasViolation(bytes: Array[Byte], expectedEol: String): Boolean =
    expectedEol match {
      case LineEndingRule.EolCrLf =>
        // CRLF期待: 孤立したLF、または後ろにLFが続かないCRがあれば違反
        bytes.indices.exists { i =>
          (bytes(i) == '\n' && (i == 0 || bytes(i - 1) != '\r')) ||
          (bytes(i) == '\r' && (i == bytes.length - 1 || bytes(i + 1) != '\n'))
        }
      case _ =>
        // LF期待: CRが1つでもあれば違反(CRLF・孤立CRの両方を検出)
        bytes.contains('\r'.toByte)
    }
}
