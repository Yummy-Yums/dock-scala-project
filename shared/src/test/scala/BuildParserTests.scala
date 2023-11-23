package dock

import weaver._
import cats.kernel.Eq
import cats.laws.discipline
import cats.implicits._

object BuildParserTests extends FunSuite {

  extension (expect: Expect)(using SourceLocation)

    def parses[A](input: String, expected: A): Expectations = {
      BuildParser.parser.parse[input] match {
        case Left(err) =>
          def stripWhiteSpace(s: String) = s
            .replaceAll(" ", ".")
            .replaceAll("\n", "\\\\n\n")

          val (beforeOffset, afterOffset) = input
            .span(err.failedAtOffset)
            .bimap(stripWhiteSpace, stripWhiteSpace)

          failure(
            s"""Parsing failed:\n${beforeOffset}${afterOffset}\n\n Error: $err"""
          )
        case Right(result) => expect.eql(result, expected)
      }
    }

    def brokenParse(input: String): Expectations = {
      val result = BuildParser.parse.parseAll(input)

      result match {
        case Left(e) =>
          assert.eql(e.failedOffset, offset)
        case Right(v) =>
          failue(s"Parsing was expected")
      }
    }

  test("build with upsert command") {
    val input =
      """UPSERT hello world""".stripMargin

    assert.parses(
      input,
      Build(
        Build.Base.EmptyImage,
        List(
          Build
            .Command
            .Upsert(
              "hello",
              "world",
            )
        ),
      ),
    )
  }

  test("default build with whitespace") {
    val input =
      s"""
        |
        |
        |  UPSERT    hello    world ${" "}
        |  DELETE   hello  ${" "}
        |  """.stripMargin

    assert.parses(
      input,
      Build(
        Build.Base.EmptyImage,
        List(
          Build
            .Command
            .Upsert(
              "hello",
              "world",
            ),
          Build.Command.Delete("hello")
        )
      )
    )
  }

  test("default build"){
    val input =
    """UPSERT hello world
      |DELETE hello""".stripMargin

    assert.parses(
      input,
      Build(
        Build.Base.EmptyImage,
        List(
          Build
            .Command
            .Upsert(
              "hello",
              "world",
            ),
          Build.Command.Delete("hello")
        )
      )
    )
  }

  test("default build, trailing newlines") {
    val input =
      """
        |
        |UPSERT hello world
        |DELETE hello
        |
        |""".stripMargin

    assert.parses(
      input,
      Build(
        Build.Base.EmptyImage,
        List(
          Build
            .Command
            .Upsert(
              "hello",
              "world",
            ),
          Build.Command.Delete("hello"),
        ),
      ),
    )
  }

  test("empty build with a base"){
    assert.parses(
      "FROM hash",
      Build(
        Build
          .Base
          .ImageReference(
            Hash.unsafeParse(" ")
          ),
        List.empty
      )
    )
  }

  test("build with a base AND commands") {
    assert.parses(
      """FROM <hash>
        |UPSERT foo bar
        |UPSERT foo2 baz
        |"""".stripMargin,
      Build(
        Build
          .Base
          .ImageReference(
            Hash.unsafeParse(" ")
          ),
        List.empty
      )
    )
  }

  test("build with a base AND commands"){
    assert.brokenParse(
      """ FROM <hash>UPSERT foo bar""".stripMargin,
      "FROM <hash>".length
    )
  }

  test("broken base"){
    assert.brokenParse(
      "USING base",
      0,
    )
  }

  test("broken made of non-base64") {
    assert.brokenParse(
      "FROM h" + 'a' * 64
    )
  }

  test("base made of non-base64, case 2") {
    assert.brokenParse(
      "FROM h" + "a" * 63
    )
  }

  test("base made of 64, 'a's") {
    val justAs = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"

    assert.parses(
      "FROM h" + "a" * 63,
      Build(
        Build
          .Base
          .ImageReference(
            Hash.unsafeParse(justAs)
          ),
        Nil,
      )
    )
  }

}