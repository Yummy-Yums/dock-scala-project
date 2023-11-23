package dock

import cats.{ApplicativeThrow, MonadThrow}
import cats.effect.kernel.{Ref, Resource}
import cats.effect.implicits.*
import cats.effect.std.UUIDGen
import dock.Build.Error.UnknownHash


trait Resolver[F[_]] {
    def resolve(build: Build): F[ResolvedBuild]
}

object Resolver {

  def apply[F[_]](using ev: Resolver[F]): Resolver[F] = ev

  def instance[F[_]: MonadThrow: Registry]: Resolver[F] = {
     val emptyHash: Hash = Hash(Vector.empty)
//     val emptySystem: SystemState = SystemState(Map.empty)

      new Resolver[F] {

        private val resolveCommand: Build.Command => ResolvedBuild.Command = {
          case Build.Command.Upsert(k, v) => ResolvedBuild.Command.Upsert(k, v)
          case Build.Command.Delete(k) => ResolvedBuild.Command.Delete(k)
        }

        private def resolveBase(base: Build.Base): F[ResolvedBuild] =
          base match {
            case Build.Base.EmptyImage =>
             ???
            case Build.Base.ImageReference(hash) =>
              Registry[F]
                .lookup(hash)
                .flatMap(_.liftTo[F](UnknownBase(hash)))
          }


        def resolve(build: Build): F[ResolvedBuild] = resolveBase(build.base)
          .map { sys =>
            ResolvedBuild(sys, build.commands.map(resolveCommand))
          }

      }
    }
  }
  