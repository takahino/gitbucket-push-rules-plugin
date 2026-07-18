package io.github.takahino.gitbucket.pushrules.util

// ブランチ名向けのglobマッチャー。
// `*` は `/` を含まない任意文字列、`**` は `/` を含む任意文字列、`?` は `/` 以外の1文字にマッチする。
// 例: `release/*` は `release/1.0` に一致するが `release/1.0/hotfix` には一致しない。
object GlobMatcher {

  def matches(pattern: String, target: String): Boolean =
    toRegex(pattern).matches(target)

  private[util] def toRegex(pattern: String): scala.util.matching.Regex = {
    val sb = new StringBuilder
    var i = 0
    while (i < pattern.length) {
      pattern.charAt(i) match {
        case '*' if i + 1 < pattern.length && pattern.charAt(i + 1) == '*' =>
          sb.append(".*")
          i += 1
        case '*' => sb.append("[^/]*")
        case '?' => sb.append("[^/]")
        case c if "\\.[]{}()+-^$|".indexOf(c.toInt) >= 0 => sb.append('\\').append(c)
        case c => sb.append(c)
      }
      i += 1
    }
    ("^" + sb.toString + "$").r
  }
}
