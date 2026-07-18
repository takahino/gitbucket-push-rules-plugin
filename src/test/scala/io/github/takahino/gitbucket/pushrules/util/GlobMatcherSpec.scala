package io.github.takahino.gitbucket.pushrules.util

import org.scalatest.funsuite.AnyFunSuite

class GlobMatcherSpec extends AnyFunSuite {

  test("完全一致パターン") {
    assert(GlobMatcher.matches("main", "main"))
    assert(!GlobMatcher.matches("main", "main2"))
    assert(!GlobMatcher.matches("main", "master"))
  }

  test("* は / を含まない任意文字列にマッチする") {
    assert(GlobMatcher.matches("release/*", "release/1.0"))
    assert(GlobMatcher.matches("release/*", "release/"))
    assert(!GlobMatcher.matches("release/*", "release/1.0/hotfix"))
    assert(!GlobMatcher.matches("release/*", "releases/1.0"))
  }

  test("** は / を含む任意文字列にマッチする") {
    assert(GlobMatcher.matches("**", "main"))
    assert(GlobMatcher.matches("**", "release/1.0/hotfix"))
    assert(GlobMatcher.matches("feature/**", "feature/foo/bar"))
    assert(!GlobMatcher.matches("feature/**", "hotfix/foo"))
  }

  test("? は / 以外の1文字にマッチする") {
    assert(GlobMatcher.matches("v?", "v1"))
    assert(!GlobMatcher.matches("v?", "v10"))
    assert(!GlobMatcher.matches("a?b", "a/b"))
  }

  test("正規表現メタ文字がエスケープされる") {
    assert(GlobMatcher.matches("release-1.0", "release-1.0"))
    assert(!GlobMatcher.matches("release-1.0", "release-1x0"))
    assert(GlobMatcher.matches("fix(1)", "fix(1)"))
    assert(GlobMatcher.matches("a+b", "a+b"))
    assert(!GlobMatcher.matches("a+b", "aab"))
  }
}
