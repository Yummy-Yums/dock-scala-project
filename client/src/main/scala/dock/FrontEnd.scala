package dock

import cats.Eq
import com.monovore.decline.Opts
import fs2.io.file.Path

object FrontEnd {

  final case class CLIRun(
    command: CLICommand,
    options: CLIOptions
  )

  object CLIRun {
    def remote(command: CLICommand): CLIRun = CLIRun(command, CLIOptions(standalone = false))

    given Eq[CLIRun] = Eq.fromUniversalEquals
  }

  enum CLICommand {
    case Build(context: Path)
    case Run(hash: dock.Hash)
    case List
  }

  object CLICommand {
    given Eq[CLICommand] = Eq.fromUniversalEquals
  }

  final case class CLIOptions(server: Boolean)

  val parseCLICommand: Opts[CLICommand] = {

    val build =
      Opts
        .subcommand("build", "Build an image")(
          Opts.argument[Path]("path")
        )
        .map(CLICommand.Build(_))

    val run =
      Opts
        .subcommand("run", "Run built image")(
          Opts
            .argument[String]("hash")
            .mapValidated(
              dock.Hash
                .parse(_)
                .toValidatedNel
            )
            .map(CLICommand.Run(_))
        )

    val list = Opts.subcommand("list", "List known images")(Opts(CLICommand.List))

    build <+> run <+> list

    val parseCLIRun: Opts[CLIRun] =  {
      (
        parseCLICommand,
        Opts.flag("standalone", "Run without server")
          .orElse
          .map(CLIOptions(_))
        ).map(CLIRun.apply)

    }

  }
}
