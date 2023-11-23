package dock

import cats.Id
import cats.catsInstancesForId
import munit.{CatsEffectSuite, ScalaCheckSuite}
import org.scalacheck.Prop.forAll
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import dock.Arbitraries.given_Arbitrary_Hash
import cats.effect.IO
import weaver.*
import weaver.scalacheck.Checkers
import cats.implicits._


object RegistryTests extends SimpleIOSuite with Checkers {

  given Hasher[F] = Hasher.sha256Hasher[F]
  val registryR = Registry.instance[IO]

  test("save + lookup returns the same system") {
    forall { (system: SystemState) =>
      registryR.flatMap { registry =>
        for {
          hash <- registry.save(system)
          result <- registry.lookup(hash)
        } yield assert(result.contains(system))
      }
    }
  }

//  test("save is idempotent") {
//    forall { (system: SystemState) =>
//
//      registryR.use { registry =>
//        for {
//          hash <- registry.save(system)
//          hash2 <- registry.save(system)
//        } yield assert(hash == hash2)
//      }
//    }
//  }

  test("save is not affected by other writes") {
    forall { (system: SystemState) =>

      registryR.flatMap { registry =>
        for {
          hash <- registry.save(system)
          hash2 <- registry.save(system)
        } yield assert(hash == hash2)
      }
    }
  }

  test("lookup is idempotent") {
    forall {
      (
        systems: List[SystemState],
        moreSystems: List[SystemState],
        hash: Hash
      ) =>
        registryR.flatMap { registry =>
          for {
            _ <- systems.traverse_(registry.save)
            result1 <- registry.save(system)

            _ <- moreSystems.traverse_(registry.save)
            result2 <- registry.lookup(hash)
          } yield assert(result1 == result2)
        }
    }
  }

  test("list on an empty registry") {
    registryR.flatMap { registry =>

      for {
        result <- registry.list
      } yield assert(result.isEmpty)

    }
  }

  test("save + list returns saved"){
    forall { (systems: List[SystemState]) =>
      registryR.flatMap { registry =>
        for {
          hashes <- systems.traverse_(registry.save)
          result <- registry.list
        } yield assert(result.toSet = hashes.toSet)

      }

    }
  }
}