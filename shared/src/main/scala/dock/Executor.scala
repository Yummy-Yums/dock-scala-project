package dock

import cats.effect.Concurrent
import cats.{Applicative, Eq, Eval, Functor, Traverse}
import cats.implicits.*
import io.circe.Codec
import sttp.tapir.Schema

enum OutputEvent[+A] derives Codec.AsObject, Schema {
  case LogMessage(text: String)
  case Result(value: A)
  case Halt(e: GenericServerError)
  case Failure(e: GenericServerError)
}

object OutputEvent {

  given [A: Codec]: Codec.AsObject[OutputEvent[A]] = Codec.AsObject.derived

  type X = scala.Matchable

  given [A: Eq]: Eq[OutputEvent[A]] = {
    case (LogMessage(a), LogMessage(b)) => a === b
    case (Result(a), Result(b))         => a === b
    case (Failure(a), Failure(b))       => a === b
    case _ => false
  }

  given Functor[OutputEvent] =
    new Traverse[OutputEvent] {
      def map[A, B](fa: OutputEvent[A])(f: A => B): OutputEvent[B] =
        fa match {
          case Result(a) => Result(f(a))
          case Failure(a) => Failure(a)
          case LogMessage(msg) => LogMessage(msg)
        }

      def traverse[G[_]: Applicative, A, B](fa: OutputEvent[A])(f: A => G[B]): G[OutputEvent[B]] =
        fa match {
          case Result(v)  => f(v).map(Result(_))
          case LogMessage(msg) => LogMessage(msg).pure[G]
          case Failure(e) => Failure(e).pure[G]
        }

      def foldLeft[A, B](fa: OutputEvent[A], b: B)(f: (B, A) => B): B =
        fa match {
          case Result(v) => f(b, v)
          case LogMessage(_) | Failure(_) => b
        }

      def foldRight[A, B](fa: OutputEvent[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] = fa match {
        case Result(v) => f(v, lb)
      }
    }

  def getResult[F[_]: Concurrent, A](
    stream: fs2.Stream[F, OutputEvent[A]]
  ): F[A] =
    stream
      .collectFirst { case Result(a) => a}
      .compile
      .lastOrError

}

trait Executor[F[_]] {
  def build(build: Build): fs2.Stream[F, OutputEvent[Hash]]
  def run(hash: Hash): F[SystemState]
  def listImages: F[List[Hash]]
}


object Executor {
  def apply[F[_]](using F : Executor[F]): Executor[F] = F
}
