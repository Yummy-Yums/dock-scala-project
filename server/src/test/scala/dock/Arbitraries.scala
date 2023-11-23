package dock

import org.scalacheck.{Arbitrary, Gen}

object Arbitraries {
  given Arbitrary[SystemState] = Arbitrary(Gen.resultOf(SystemState.apply))

  given Arbitrary[Command] = Arbitrary {
    Gen.oneOf(

      Gen.resultOf(Command),
      Gen.resultOf(Build.Command.Delete),

    )
  }

  given arbResolvedCommand: Arbitrary[Command] = Arbitrary {
    Gen.oneOf(

      Gen.resultOf(Build.Command.Upsert),
      Gen.resultOf(Build.Command.Delete),

    )
  }

  given Arbitrary[Hash] = Arbitrary {
    Gen.resultOf(Hash.apply)
  }
}
