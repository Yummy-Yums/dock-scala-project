package dock

import cats.effect.{IO, IOApp}
import com.comcast.ip4s.{host, port}
import org.http4s.HttpApp
import org.http4s.client.middleware.Logger
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import org.typelevel.log4cats.noop.NoOpLogger

object Main extends IOApp.Simple {

  given Logger[IO] = NoOpLogger[IO]

  def run: IO[Unit] =
    ServerSideExecutor.module[IO].flatMap { exec =>
      EmberServerBuilder
        .default[IO]
        .withHost(host"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp {
          Routing.instance[IO](exec)
        }
        .build
    }
      .useForever


}
