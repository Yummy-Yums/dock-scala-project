package dock

import sttp.model.StatusCode
import sttp.tapir.Endpoint
import sttp.capabilities.fs2.Fs2Streams
import cats.effect.kernel.Sync
import io.circe.syntax._
import scala.concurrent.duration._
import org.typelevel.log4cats.Logger
import io.circe.Codec as CirceCodec

object protocol {

  import sttp.tapir.*
  import sttp.tapir.generic.auto.*
  import sttp.tapir.json.circe.*

  private val base = infallibleEndpoint.in("api")

  private def outputEventStream[F[_]: Sync: Logger, A: CirceCodec: Schema] =
    streamBody(Fs2Streams[F])(
      summon[Schema[List[OutputEvent[OutputEvent[A]]]]], CodecFormat.Json()
    )
      .map { bytes =>
        bytes
          .through(io.circe.fs2.byteArrayParser)
          .through(io.circe.fs2.decoder[F, OutputEvent[Hash]])
        io.circe.fs2.decoder[F, OutputEvent[Hash]]

      } { events =>
        fs2
          .Stream
          .emit("[")
          .append(
            events
              .handleErrorWith { e =>
                fs2.Stream.exec(Logger[F].error(e)("Response stream error"))
                  ++
                  fs2
                    .Stream
                    .emit(OutputEvent.Failure(GenericServerError("Response stream error")))
              }
              .map(_.asJson.noSpaces)
              .instersperse(",")
          )
          .append(fs2.Stream.emit("]"))
          .through(fs2.text.utf8.encode)

      }


  def build[
    F[_]: Sync: Logger
  ]: Endpoint[Unit, Build, Build.Error, fs2.Stream[
      F,
      OutputEvent[Either[Build.Error, Hash]]
    ], Fs2Streams[F]] = base
    .put
    .in("build")
    .in(jsonBody[Build])
    .out(outputEventStream[F, Either[Build.Error, Hash]])
    .errorOut(statusCode(StatusCode.UnprocessableEntity).and(jsonBody[Build.Error]))

  val run : Endpoint[Unit, Hash, RunError, SystemState, Any] = base
    .post
    .in("run")
    .in(jsonBody[Hash])
    .out(jsonBody[SystemState])
    .errorOut(statusCode(StatusCode.UnprocessableEntity).and(jsonBody[RunError]))

  val listImages: Endpoint[Unit, Unit, Nothing, List[Hash], Any] = base
    .get
    .in("images")
    .out(jsonBody[List[Hash]])


}


