import gitbucket.core.controller.Context
import gitbucket.core.plugin.{Link, ReceiveHook, RepositoryHook}
import gitbucket.core.service.RepositoryService.RepositoryInfo
import io.github.gitbucket.solidbase.migration.LiquibaseMigration
import io.github.gitbucket.solidbase.model.Version
import io.github.takahino.gitbucket.pushrules.controller.PushRulesController
import io.github.takahino.gitbucket.pushrules.hook.{BranchPushRuleHook, LineEndingRuleHook, PushRulesRepositoryHook}

class Plugin extends gitbucket.core.plugin.Plugin {

  override val pluginId = "push-rules"

  override val pluginName = "Push Rules Plugin"

  override val description =
    "Provides configurable push rules per repository (branch push restriction and line ending check)."

  override val versions = List(
    new Version("1.0.0", new LiquibaseMigration("update/gitbucket-push-rules_1.0.0.xml"))
  )

  override val controllers = Seq(
    "/*" -> new PushRulesController()
  )

  override val receiveHooks: Seq[ReceiveHook] = Seq(
    new BranchPushRuleHook(),
    new LineEndingRuleHook()
  )

  override val repositoryHooks: Seq[RepositoryHook] = Seq(new PushRulesRepositoryHook())

  override val repositorySettingTabs = Seq((repository: RepositoryInfo, context: Context) =>
    Some(Link("push-rules", "Push Rules", "settings/push-rules"))
  )
}
