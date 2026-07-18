package io.github.takahino.gitbucket.pushrules.model

trait LineEndingRuleComponent { self: gitbucket.core.model.Profile =>
  import profile.api._
  // self-type(gitbucket.core.model.Profile)の implicit dateColumnType を implicit scope に載せる
  import self._

  lazy val LineEndingRules = TableQuery[LineEndingRules]

  class LineEndingRules(tag: Tag) extends Table[LineEndingRule](tag, "LINE_ENDING_RULE") {
    val ruleId = column[Int]("RULE_ID", O.AutoInc)
    val userName = column[String]("USER_NAME")
    val repositoryName = column[String]("REPOSITORY_NAME")
    val dirPrefix = column[String]("DIR_PREFIX")
    val extensions = column[String]("EXTENSIONS")
    val expectedEol = column[String]("EXPECTED_EOL")
    val enabled = column[Boolean]("ENABLED")
    val registeredDate = column[java.util.Date]("REGISTERED_DATE")
    def * =
      (ruleId, userName, repositoryName, dirPrefix, extensions, expectedEol, enabled, registeredDate)
        .mapTo[LineEndingRule]
  }
}

case class LineEndingRule(
  ruleId: Int = 0,
  userName: String,
  repositoryName: String,
  dirPrefix: String,
  extensions: String,
  expectedEol: String,
  enabled: Boolean,
  registeredDate: java.util.Date
)

object LineEndingRule {
  val EolLf = "LF"
  val EolCrLf = "CRLF"
  val ValidEols: Seq[String] = Seq(EolLf, EolCrLf)
}
