package io.github.takahino.gitbucket.pushrules.controller

import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.{AccountService, RepositoryService}
import gitbucket.core.util.OwnerAuthenticator
import gitbucket.core.util.Implicits._
import io.github.takahino.gitbucket.pushrules.model.LineEndingRule
import io.github.takahino.gitbucket.pushrules.service.PushRuleService

class PushRulesController
    extends ControllerBase
    with PushRuleService
    with AccountService
    with RepositoryService
    with OwnerAuthenticator {

  get("/:owner/:repository/settings/push-rules")(ownerOnly { repository =>
    io.github.takahino.gitbucket.pushrules.html.settings(
      getBranchPushRules(repository.owner, repository.name),
      getLineEndingRules(repository.owner, repository.name),
      repository,
      flash.get("info"),
      flash.get("error")
    )
  })

  post("/:owner/:repository/settings/push-rules/branch/new")(ownerOnly { repository =>
    val pattern = params.get("branchPattern").map(_.trim).getOrElse("")
    val users = params
      .get("allowedUsers")
      .map(_.split(",").map(_.trim).filter(_.nonEmpty).toSeq)
      .getOrElse(Nil)
    val unknownUsers = users.filter(getAccountByUserName(_).isEmpty)

    if (pattern.isEmpty) {
      flash.update("error", "Branch pattern is required.")
    } else if (users.isEmpty) {
      flash.update("error", "At least one allowed user is required.")
    } else if (unknownUsers.nonEmpty) {
      flash.update("error", s"Unknown user(s): ${unknownUsers.mkString(", ")}")
    } else {
      addBranchPushRule(repository.owner, repository.name, pattern, users)
      flash.update("info", "Branch push rule has been added.")
    }
    redirect(s"/${repository.owner}/${repository.name}/settings/push-rules")
  })

  post("/:owner/:repository/settings/push-rules/branch/:ruleId/delete")(ownerOnly { repository =>
    params.get("ruleId").flatMap(_.toIntOption).foreach { ruleId =>
      deleteBranchPushRule(repository.owner, repository.name, ruleId)
      flash.update("info", "Branch push rule has been deleted.")
    }
    redirect(s"/${repository.owner}/${repository.name}/settings/push-rules")
  })

  post("/:owner/:repository/settings/push-rules/eol/new")(ownerOnly { repository =>
    val dirPrefix = params.get("dirPrefix").map(_.trim.stripPrefix("/").stripSuffix("/")).getOrElse("")
    val extensions = params
      .get("extensions")
      .map(_.split(",").map(_.trim.stripPrefix(".")).filter(_.nonEmpty).mkString(","))
      .getOrElse("")
    val expectedEol = params.get("expectedEol").map(_.trim.toUpperCase).getOrElse("")

    if (extensions.isEmpty) {
      flash.update("error", "At least one extension is required.")
    } else if (!LineEndingRule.ValidEols.contains(expectedEol)) {
      flash.update("error", "Expected line ending must be LF or CRLF.")
    } else {
      addLineEndingRule(repository.owner, repository.name, dirPrefix, extensions, expectedEol)
      flash.update("info", "Line ending rule has been added.")
    }
    redirect(s"/${repository.owner}/${repository.name}/settings/push-rules")
  })

  post("/:owner/:repository/settings/push-rules/eol/:ruleId/delete")(ownerOnly { repository =>
    params.get("ruleId").flatMap(_.toIntOption).foreach { ruleId =>
      deleteLineEndingRule(repository.owner, repository.name, ruleId)
      flash.update("info", "Line ending rule has been deleted.")
    }
    redirect(s"/${repository.owner}/${repository.name}/settings/push-rules")
  })
}
