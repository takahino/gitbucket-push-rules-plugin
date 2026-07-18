package io.github.takahino.gitbucket.pushrules.hook

import gitbucket.core.model.Profile._
import gitbucket.core.plugin.RepositoryHook
import io.github.takahino.gitbucket.pushrules.service.PushRuleService
import profile.api._

/** リポジトリの削除/リネーム/移管に追従してルールを掃除する */
class PushRulesRepositoryHook extends RepositoryHook with PushRuleService {

  override def deleted(owner: String, repository: String)(implicit session: Session): Unit =
    deleteAllRules(owner, repository)

  override def renamed(owner: String, repository: String, newRepository: String)(implicit
    session: Session
  ): Unit =
    renameRepositoryRules(owner, repository, newRepository)

  override def transferred(owner: String, newOwner: String, repository: String)(implicit
    session: Session
  ): Unit =
    transferRepositoryRules(owner, newOwner, repository)
}
