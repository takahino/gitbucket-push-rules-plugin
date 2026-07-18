package io.github.takahino.gitbucket.pushrules.service

import io.github.takahino.gitbucket.pushrules.model._, Profile._
import profile.blockingApi._

trait PushRuleService {

  // ==== Branch push rules ====

  def getBranchPushRules(owner: String, repository: String)(implicit
    s: Session
  ): List[(BranchPushRule, List[String])] = {
    val rules = BranchPushRules
      .filter(t => t.userName === owner.bind && t.repositoryName === repository.bind)
      .sortBy(_.ruleId)
      .list
    val users = BranchPushRuleAllowedUsers
      .filter(_.ruleId inSetBind rules.map(_.ruleId))
      .list
      .groupBy(_.ruleId)
    rules.map(rule => rule -> users.getOrElse(rule.ruleId, Nil).map(_.allowedUserName))
  }

  def getEnabledBranchPushRules(owner: String, repository: String)(implicit
    s: Session
  ): List[(BranchPushRule, List[String])] =
    getBranchPushRules(owner, repository).filter(_._1.enabled)

  def addBranchPushRule(owner: String, repository: String, branchPattern: String, allowedUsers: Seq[String])(implicit
    s: Session
  ): Unit = {
    val ruleId = (BranchPushRules returning BranchPushRules.map(_.ruleId)) insert BranchPushRule(
      userName = owner,
      repositoryName = repository,
      branchPattern = branchPattern,
      enabled = true,
      registeredDate = new java.util.Date()
    )
    allowedUsers.distinct.foreach { user =>
      BranchPushRuleAllowedUsers insert BranchPushRuleAllowedUser(ruleId, user)
    }
  }

  def deleteBranchPushRule(owner: String, repository: String, ruleId: Int)(implicit s: Session): Unit = {
    // 他リポジトリのルールIDを指定されても消さないよう、所属確認してから削除する
    BranchPushRules
      .filter(t => t.ruleId === ruleId.bind && t.userName === owner.bind && t.repositoryName === repository.bind)
      .firstOption
      .foreach { _ =>
        BranchPushRuleAllowedUsers.filter(_.ruleId === ruleId.bind).delete
        BranchPushRules.filter(_.ruleId === ruleId.bind).delete
      }
  }

  // ==== Line ending rules ====

  def getLineEndingRules(owner: String, repository: String)(implicit s: Session): List[LineEndingRule] =
    LineEndingRules
      .filter(t => t.userName === owner.bind && t.repositoryName === repository.bind)
      .sortBy(_.ruleId)
      .list

  def getEnabledLineEndingRules(owner: String, repository: String)(implicit s: Session): List[LineEndingRule] =
    getLineEndingRules(owner, repository).filter(_.enabled)

  def addLineEndingRule(owner: String, repository: String, dirPrefix: String, extensions: String, expectedEol: String)(
    implicit s: Session
  ): Unit =
    LineEndingRules insert LineEndingRule(
      userName = owner,
      repositoryName = repository,
      dirPrefix = dirPrefix,
      extensions = extensions,
      expectedEol = expectedEol,
      enabled = true,
      registeredDate = new java.util.Date()
    )

  def deleteLineEndingRule(owner: String, repository: String, ruleId: Int)(implicit s: Session): Unit =
    LineEndingRules
      .filter(t => t.ruleId === ruleId.bind && t.userName === owner.bind && t.repositoryName === repository.bind)
      .delete

  // ==== Repository lifecycle ====

  def deleteAllRules(owner: String, repository: String)(implicit s: Session): Unit = {
    val ruleIds = BranchPushRules
      .filter(t => t.userName === owner.bind && t.repositoryName === repository.bind)
      .map(_.ruleId)
      .list
    BranchPushRuleAllowedUsers.filter(_.ruleId inSetBind ruleIds).delete
    BranchPushRules.filter(t => t.userName === owner.bind && t.repositoryName === repository.bind).delete
    LineEndingRules.filter(t => t.userName === owner.bind && t.repositoryName === repository.bind).delete
  }

  def renameRepositoryRules(owner: String, repository: String, newRepository: String)(implicit s: Session): Unit = {
    BranchPushRules
      .filter(t => t.userName === owner.bind && t.repositoryName === repository.bind)
      .map(_.repositoryName)
      .update(newRepository)
    LineEndingRules
      .filter(t => t.userName === owner.bind && t.repositoryName === repository.bind)
      .map(_.repositoryName)
      .update(newRepository)
  }

  def transferRepositoryRules(owner: String, newOwner: String, repository: String)(implicit s: Session): Unit = {
    BranchPushRules
      .filter(t => t.userName === owner.bind && t.repositoryName === repository.bind)
      .map(_.userName)
      .update(newOwner)
    LineEndingRules
      .filter(t => t.userName === owner.bind && t.repositoryName === repository.bind)
      .map(_.userName)
      .update(newOwner)
  }
}
