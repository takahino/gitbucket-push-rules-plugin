package io.github.takahino.gitbucket.pushrules.hook

import gitbucket.core.model.Profile._
import gitbucket.core.model.Role
import gitbucket.core.plugin.ReceiveHook
import gitbucket.core.service.{AccountService, RepositoryService}
import io.github.takahino.gitbucket.pushrules.service.PushRuleService
import io.github.takahino.gitbucket.pushrules.util.GlobMatcher
import org.eclipse.jgit.transport.{ReceiveCommand, ReceivePack}
import profile.api._

/**
 * ブランチpush制限フック。
 * ルールにマッチしたブランチへの直接pushは「許可ユーザー」のみ、
 * Web UIからのPRマージ(mergePullRequest=true)は「マージ許可ユーザー」のみに制限する。
 * 管理者(オーナー/システム管理者/グループマネージャー/ADMINロールのコラボレータ)は
 * どちらも常にバイパスする。
 */
class BranchPushRuleHook extends ReceiveHook with PushRuleService with AccountService with RepositoryService {

  override def preReceive(
    owner: String,
    repository: String,
    receivePack: ReceivePack,
    command: ReceiveCommand,
    pusher: String,
    mergePullRequest: Boolean
  )(implicit session: Session): Option[String] = {
    val branch = command.getRefName.stripPrefix("refs/heads/")
    if (branch == command.getRefName) {
      // タグやnotes等、ブランチ以外への操作は対象外
      None
    } else {
      val matched = getEnabledBranchPushRules(owner, repository)
        .filter(r => GlobMatcher.matches(r.rule.branchPattern, branch))
      if (matched.isEmpty) {
        None
      } else if (mergePullRequest) {
        if (matched.exists(_.mergeUsers.contains(pusher)) || isBypassAllowed(owner, repository, pusher)) {
          None
        } else {
          Some(s"Merging into branch '$branch' is restricted by push rules. Contact the repository owner.")
        }
      } else {
        if (matched.exists(_.allowedUsers.contains(pusher)) || isBypassAllowed(owner, repository, pusher)) {
          None
        } else {
          Some(s"Push to branch '$branch' is restricted by push rules. Contact the repository owner.")
        }
      }
    }
  }

  // 本体 ProtectedBranchService の isAdministrator と同基準 + システム管理者
  private def isBypassAllowed(owner: String, repository: String, pusher: String)(implicit
    session: Session
  ): Boolean =
    pusher == owner ||
      getAccountByUserName(pusher).exists(_.isAdmin) ||
      getGroupMembers(owner).exists(gm => gm.userName == pusher && gm.isManager) ||
      getCollaborators(owner, repository).exists { case (collaborator, isGroup) =>
        collaborator.role == Role.ADMIN.name && {
          if (isGroup) {
            getGroupMembers(collaborator.collaboratorName).exists(_.userName == pusher)
          } else {
            collaborator.collaboratorName == pusher
          }
        }
      }
}
