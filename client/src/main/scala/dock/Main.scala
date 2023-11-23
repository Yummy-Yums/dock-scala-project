package dock

import cats.{Applicative, FlatMap, Functor}
import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.client.middleware.Logger
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import sttp.tapir.client.http4s.Http4sClientInterpreter
import cats.effect.*
import cats.implicits.*
import com.monovore.decline.*
import com.monovore.decline.effect.*
import fs2.io.file.{Files, Path}
import dock.BuildReader
import dock.Executor
import dock.ClientSideExecutor
import org.typelevel.log4cats.Logger


object Main extends CommandIOApp("dock", "Command line interface for dock") {

  import dock.FrontEnd._

  given logger = Slf4jLogger.getLogger[IO]

  def exec[F[_]: Async: Logger]: Resource[F, Executor[F]] = EmberClientBuilder
    .default[F]
    .build
    .use { client =>

      given Http4sClientInterpreter[IO] = Http4sClientInterpreter[IO]()

      ClientSideExecutor.instance[IO](client)

    }

  def convertCommand[F[_] : BuildReader: Applicative]: CLICommand => F[dock.Command] = {

    case CLICommand.Build(ctx) =>
      BuildReader[F].read(
          Path.fromNioPath(ctx) / "build.steve"
        )
        .map(dock.Command.Build(_))
    case CLICommand.Run(hash) => dock.Command.Run(hash).pure[F]
    case CLICommand.List => dock.Command.ListImages.pure[F]

  }

  def eval[F[_]: MonadCancelThrow](exec: Executor[F])(using fs2.Compiler[F, F]): dock.Command => F[String] = {

    case dock.Command.Build(build) =>
      OutputEvent
        .getResult{
          exec
            .build(build)
            .evalTap {
              case OutputEvent.LogMessage(msg) => cats.effect.std.Console[F].println("INFO: " + msg)
              case _ => Applicative[F].unit
            }
        }
        .rethrow
        .map { hash =>
          hash.toHex
        }

    case dock.Command.Run(hash) =>
      exec.run(hash).map { state =>
        "System state: \n\n" + state.prettyPrint
      }
    case dock.Command.ListImages =>
      exec.listImages.map { images =>
        images.mkString("\n")

      }
  }

  given BuildReader[IO] = BuildReader.instance

  val main: Opts[IO[ExitCode]] = FrontEnd.parseCLIRun.map { case run =>
    convertCommand[IO](_)(run.command)
      .flatMap { cmd =>

        val runServer =
          if (run.options.standalone)
            dock.server.Main.serve.surroundK
          else
            FunctionK.id

        runServer {
          exec[IO].use(eval[IO](_)(cmd))
        }
      }
      .flatMap(IO.println(_))
      .as(ExitCode.Success)
  }


}

