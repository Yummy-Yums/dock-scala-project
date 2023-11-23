package dock

import cats.effect.kernel.{Ref, Resource, Sync}
import cats.effect.std.UUIDGen
import cats.implicits.*
import cats.{Applicative, ApplicativeThrow, MonadThrow}


object ServerSideExecutor {

  def instance[F[_] : Interpreter : Resolver : Registry : MonadThrow]: Executor[F] =
    new Executor[F] {

      def build(build: Build): fs2.Stream[F, OutputEvent[Either[Build.Error, Hash]]] =
        fs2
          .Stream
          .eval {
            Resolver[F]
              .resolve(build)
              .attemptNarrow[Build.Error]
          }
          .flatMap { resolvedBuild =>
            resolvedBuild match {
              case Right(v) => Interpreter[F].interpret(v)
              case Left(e) => fs2.Stream.emit(Output.Result(Left(e)))
            }
          }
          .evalMap(_.traverse(_.traverse(Registry[F].save)))

      def run(hash: Hash): F[SystemState] = Registry[F]
          .lookup(hash)
          .flatMap(_.liftTo[F](RunError.UnknownHash(hash)))

      val listImages: F[List[Hash]] = Registry[F].list
    }

  def module[F[_] : Sync]: Resource[F, Executor[F]] = {
    val unit = Applicative[F].unit.toResource

    given Interpreter[F] = Interpreter.instance[F]

    given Hasher[F] = Hasher.sha256Hasher[F]

    for {
      given Registry[F] <- Registry.instance[F].toResource
      _ <- unit
      given Resolver[F] <- Resolver.instance[F]
    } yield instance[F]

  }

}





