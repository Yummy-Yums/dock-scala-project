package dock

import cats.MonadThrow
import fs2.io.file.{Files, Path}

trait BuildReader[F[_]] {
  def read(buildFile: Path): F[Build]
}

object BuildReader {

  def apply[F[_]](using F: BuildReader[F]): BuildReader[F] = F

  def instance[F[_]: Files: MonadThrow](using fs2.Compiler[F, F]): BuildReader[F] =
    buildFile =>
      Files[F]
        .readAll(buildFile)
        .through(fs2.text.utf8.decode[F])
        .compile
        .string
        .flatMap(
          BuildParser.parser.parseAll(_).liftTo[F]
        )

  final case class ParsingFailure(e: Parser.Error) extends Exception(e.toString())
}
