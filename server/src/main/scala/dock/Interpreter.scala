package dock

import cats.implicits.*
import cats.Applicative
import cats.data.State
import cats.effect.kernel.Ref
import monocle.syntax.all.*

trait Interpreter[F[_]] {
  def interpret(build: ResolvedBuild): fs2.Stream[F, OutputEvent[SystemState]]
}

object Interpreter {

  def apply[F[_]](using F: Interpreter[F]): Interpreter[F] = F

  def instance[F[_]: Applicative]: Interpreter[F] =
    new Interpreter[F]{
      private def transition(stateRef: Ref[F, SystemState]): ResolvedBuild.Command => OutputEvent.LogMessage[SystemState, Unit] = {
          case ResolvedBuild.Command.Upsert(k, v) =>
            stateRef
              .update(_.upsert(k, v))
              .as(OutputEvent.LogMessage(s"Upserting $k: $v"))

          case ResolvedBuild.Command.Delete(k) =>
            stateRef
              .update(_.delete(k))
              .as(OutputEvent.LogMessage(s"Deleting $k"))
        }

      def interpret(
          build: ResolvedBuild
      ): fs2.Stream[F, OutputEvent[Either[Build.Error, SystemState]]] = fs2
            .Stream
            .eval(Ref[F].of(build.base))
            .flatMap { stateRef =>
             val lhs = fs2
                .Stream
                .emits(build.commands)
                .evalMap(transition(stateRef))

             val rhs = fs2.Stream.eval(OutputEvent.Result(stateRef.get))

             lhs ++ rhs
        }
        .map(_.map(_.asRight))

    }

  }