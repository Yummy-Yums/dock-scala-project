package dock

import cats.data.Kleisli
import cats.effect.kernel.{Async, Sync}
import org.http4s.HttpApp
import org.http4s.dsl.Http4sDsl
import sttp.model.StatusCode
import sttp.tapir.EndpointIO.annotations.statusCode
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.json.circe.*
import sttp.tapir.*
import cats.implicits.*
import org.typelevel.log4cats.Logger
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.server.interceptor.exception.ExceptionHandler

object Routing {
    def instance[F[_]: Async: Logger](exec: Executor[F]): HttpApp[F] = {
      val endpoints: List[ServerEndpoint[Fs2Streams[F], F]] = List(
        protocol.build.serverLogicSuccess(exec.build(_).pure[F]),
        protocol.run.serverLogicRecoverErrors(exec.run),
        protocol.listImages.serverLogicSuccess(_ => exec.listImages)
      )

      Http4sServerInterpreter[F](
        Http4sServerOptions
          .customiseInterceptors[F]
          .exceptionHandler { ex =>
            ExceptionHandler.pure {
              ValuedEndpointOutput(
                jsonBody[GenericServerError].and(statusCode(StatusCode.InternalServerError)),
                GenericServerError("server failed")
              ).some
            }
          }
          .options
      )
        .toRoutes(endpoints)
        .orNotFound



    }
}
