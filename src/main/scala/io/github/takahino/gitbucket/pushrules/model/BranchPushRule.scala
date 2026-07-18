package io.github.takahino.gitbucket.pushrules.model

trait BranchPushRuleComponent { self: gitbucket.core.model.Profile =>
  import profile.api._
  // self-type(gitbucket.core.model.Profile)の implicit dateColumnType を implicit scope に載せる
  import self._

  lazy val BranchPushRules = TableQuery[BranchPushRules]
  lazy val BranchPushRuleAllowedUsers = TableQuery[BranchPushRuleAllowedUsers]

  class BranchPushRules(tag: Tag) extends Table[BranchPushRule](tag, "BRANCH_PUSH_RULE") {
    val ruleId = column[Int]("RULE_ID", O.AutoInc)
    val userName = column[String]("USER_NAME")
    val repositoryName = column[String]("REPOSITORY_NAME")
    val branchPattern = column[String]("BRANCH_PATTERN")
    val enabled = column[Boolean]("ENABLED")
    val registeredDate = column[java.util.Date]("REGISTERED_DATE")
    def * =
      (ruleId, userName, repositoryName, branchPattern, enabled, registeredDate).mapTo[BranchPushRule]
  }

  class BranchPushRuleAllowedUsers(tag: Tag) extends Table[BranchPushRuleAllowedUser](tag, "BRANCH_PUSH_RULE_ALLOWED_USER") {
    val ruleId = column[Int]("RULE_ID")
    val allowedUserName = column[String]("ALLOWED_USER_NAME")
    def * = (ruleId, allowedUserName).mapTo[BranchPushRuleAllowedUser]
  }
}

case class BranchPushRule(
  ruleId: Int = 0,
  userName: String,
  repositoryName: String,
  branchPattern: String,
  enabled: Boolean,
  registeredDate: java.util.Date
)

case class BranchPushRuleAllowedUser(
  ruleId: Int,
  allowedUserName: String
)
