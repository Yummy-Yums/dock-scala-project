package dock

import cats.Applicative
import cats.effect.Resource
import cats.effect.kernel.{MonadCancelThrow, Sync}
import org.http4s.{Status, Uri}
import org.http4s.Uri.{Authority, RegName}
import org.http4s.client.Client
import sttp.tapir.client.http4s.Http4sClientInterpreter
import cats.implicits.*
import cats.kernel.instances.EitherEq
import sttp.tapir.{DecodeResult, Endpoint}
import dock.GenericServerError

object ClientSideExecutor {

  def instance[F[_] : Http4sClientInterpreter: Sync: Logger](
    client: Client[F]
  )(
    using fs2.Compiler[F, F]
  ): Executor[F] =
    new Executor[F] {

      val r: List[Int] = fs2.Stream.emit(42).toList

      private def run[I, E <: Throwable, O](
        endpoint: Endpoint[Unit, I, E, O, Any],
        input: I
      ): Resource[F, O] = {
        val (req, handler) = summon[Http4sClientInterpreter[F]]
          .toRequest(endpoint, Some(uriWithLocalhost))
          .apply(input)

        client
          .run(req)
          .evalMap{
            case r if r.status == Status.InternalServerError =>
              r
                .bodyText
                .compile
                .string
                .flatMap(io.circe.parser.decode[GenericServerError](_).liftTo[F])
                .flatMap(_.raiseError[F, O])
            case r => handler(r).rethrow
          }


      }

      def build(build: Build): fs2.Stream[F, OutputEvent[Either[Build.Error, Hash]]] =
        fs2
          .Stream
          .resource(run(protocol.build)).flatten

      def run(hash: Hash): F[SystemState] = run(protocol.run, hash).use(_.pure[F])

      def listImages: F[List[Hash]] = run(protocol.listImages, ()).use(_.pure[F])

      val uriWithLocalhost: Uri = Uri (
        scheme = Some (Uri.Scheme.http),
        authority = Some (Authority (host = RegName ("localhost"), port = Some (8080) )),
      )

    }

}


