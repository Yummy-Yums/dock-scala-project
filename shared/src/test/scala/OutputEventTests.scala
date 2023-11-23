import cats.data.{Validated, ZipLazyList}
import cats.kernel.laws.discipline.EqTests
import cats.laws.discipline.TraverseTests
import dock.{GenericServerError, OutputEvent}
import org.scalacheck.{Arbitrary, Gen}
import weaver.*
import weaver.discipline.*

object OutputEventTests extends FunSuite with Discipline {

  given Arbitrary[GenericServerError] = Arbitrary(Gen.resultOf(GenericServerError.apply))
  given [A: Arbitrary]: Arbitrary[OutputEvent[A]] = Arbitrary {
    Gen.oneOf(
      Gen.resultOf(OutputEvent.Result[A].apply),
      Gen.resultOf(OutputEvent.LogMessage.apply),
      Gen.resultOf(OutputEvent.Failure.apply)
    )
  }

  checkAll(
    "Traverse[OutputEvent]",
    TraverseTests[OutputEvent]
      .traverse[Int, String, Boolean, Int, Option, Validated[Int, _]]
  )

//  checkAll(
//    "Functor[OutputEvent]",
//    TraverseTests[OutputEvent]
//      .foldable[Int, Int])

}
