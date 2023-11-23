package dock

import cats.parse.Parser as P
import cats.parse.Parser as P0

object BuildParser {
  val parser: P0[Build] = {
//    val inLineWhitespace: P[Unit] = P.charIn(" \t").void
    val whitespace: P[Unit] = P.charIn(" \t\n").void

    val command: P[Build.Command] = {

      val key: P0[String] = (Rfc5234.alpha.rep0.string | Numbers.digits0).rep0.string
      val value: P0[String] = P.until(whitespace)

      val upsert: P[Build.Command.Upsert] =
        (
          P.string("UPSERT") *> P.char(' ') *> key,
          P.char(' ') *> value,
        ).mapN(Build.Command.Upsert.apply)

      val delete: P[Build.Command.Delete] =
        (
          whitespace.rep0.with1 *> P.string("DELETE") *>
            P.string("DELETE") *>
            P.char(' ') *>
            key.map(Build.Command.Delete(_)))

      upsert | delete
    }

    val base: P0[Build.Base] = {
      val hash: P[Hash] =
        P
        .charIn(('0' to '9') ++ ('a' to 'f'))
        .rep(64)
        .string
        .map(Hash.unsafeParse)

      (P.string("FROM") *>
        P.char(' ') *>
        hash.map(Build.Base.ImageReference(_)) <*
        (P.char('\n') | P.end)
      )
        .orElse(P.pure(Build.Base.EmptyImage))
    }

    val commands: P0[List[Command]] = command.surroundedBy(P.char("\n").rep0).rep0

    (base, commands).mapN(Build.apply)
  }


}


