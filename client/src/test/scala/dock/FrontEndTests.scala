package dock

import weaver.*
import com.monovore.decline
import cats.kernel.Eq
import com.monovore.decline.Help
import dock.FrontEnd.{CLICommand, CLIRun}
import dock.FrontEnd.CLICommand.given_Eq_CLICommand
import cats.implicits._

object FrontEndTests extends FunSuite {

  given Eq[Help] = Eq.fromUniversalEquals

  def parseCommand(
    args: String*
  ) = decline.Command("test", "Test command")(FrontEnd.parseCLIRun).parse(args)

  test("build command") {
    assert.eql(
      parseCommand("build", "."),
      Right(CLIRun.remote(CLICommand.Build(Paths.get(".")))),
    )
  }

  test("run command") {
    val hashString = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    assert.eql(
      parseCommand("run", hashString),
      Right(CLIRun.remote(CLICommand.Run(Hash.parse(hashString).toOption.get))),
    )
  }

  test("list command") {
    assert.eql(parseCommand("list"), Right(CLIRun.remote(CLICommand.List)))
  }
}