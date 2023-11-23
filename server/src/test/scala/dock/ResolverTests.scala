package dock

import weaver.*
import weaver.scalacheck.Checkers
import Arbitraries.given
import cats.effect.IO
import dock.Build.Error.UnknownBase


object ResolverTests extends SimpleIOSuite with Checkers {

  given Hasher[IO] = Hasher.sha256Hasher[IO]

  val unit: Any = Applicative[IO].unit

  test("resolve(any build basing on the empty image)") {

    forall{ (
        commands: List[Build.Command]
            ) =>

      for {
        given Registry[IO] <- Registry.inMemory[IO](Map.empty)

        build = Build(Build.Base.EmptyImage, commands)
        resolved <- Resolver.instance[IO].resolve(build)
        newBase = resolved.base
      } yield assert(newBase == SystemState.empty)
    }
  }

  test("registry.save(system) >>= resolve == system") {

    forall { (system: SystemState, commands: List[Build.Command]) =>

      for {
        given Registry[IO] <- Registry.inMemory[IO](Map.empty)
        _ <- unit
        resolver = Resolver.instance[IO]
        baseHash <- Registry[IO].save(system)
        build = Build(Build.Base.ImageReference(baseHash), commands)
        resolved <- resolver.resolve(build)
        newBase = resolved.base
      } yield assert(newBase == system)

    }
  }

  test("resolve(unknown hash) fails") {
    forall { (system: SystemState, commands: List[Build.Command], hash: Hash) =>

      for {
        given Registry[IO] <- Registry.inMemory[IO](Map.empty).flatMap
        _ <- unit
        resolver = Resolver.instance[IO]
        build = Build(Base.ImageReference(hash), commands)
        resolved <- resolver.resolve(build).attempt

      } yield assert(resolved == Left(UnknownBase(hash)))

    }
  }

}