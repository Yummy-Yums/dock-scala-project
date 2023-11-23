package dock

import TestExecutor.TestResult
import cats.effect.IO
import cats.effect.kernel.Async
import munit.CatsEffectSuite
import org.http4s.client.Client
import sttp.tapir.client.http4s.Http4sClientInterpreter
import cats.implicits.*
import dock.Executor
import dock.OutputEvent
import org.http4s.HttpApp

object CompatTests extends CatsEffectSuite {


//  given Executor[IO] = ServerSideExecutor.instance[IO]
  given Http4sClientInterpreter[IO] = Http4sClientInterpreter[IO]()
//  given executorAsync: Async[Executor[IO]]  = Async[IO].compose(Executor[IO])

  val goodBuild: Build = Build.empty
  val goodBuildResult: Hash = Hash(Vector.empty)

  val unknownHash = Hash(Vector(1))

  val unknownBaseBuild = Build(
    Build.Base.ImageReference(unknownHash),
    Nil
  )

  val unexpectedFailingBuild: Build = Build(
    Build.Base.EmptyImage,
    List(dock.Build.Command.Delete("k"))
  )

  val unknownBaseError: Throwable = ??? //Build.Error.UnknownBase(unknownHash)

  val buildInternalError = new Throwable("build internal error")

  val goodHash: Hash = Hash(Vector.empty)
  val unexpectedFailingHash: Hash = Hash(Vector(42))
  val goodRunResult: SystemState = SystemState(Map.empty)

  val exec: Executor[IO] = TestExecutor.instance(
    Map(
      goodBuild -> TestResult.Success(goodBuildResult),
      unknownBaseBuild -> TestResult.Failure(buildInternalError),
      unexpectedFailingBuild -> TestResult.Crash(new Throwable("build internal error"))
    ),
    Map(
      goodHash -> TestResult.Success(goodRunResult),
      unexpectedFailingHash -> TestResult.Crash(new Throwable("run internal error"))
    )
  )


//  given fa: Async[IO] = Async[IO].compose(Executor[IO])

  val client: Executor[IO] = ClientSideExecutor.instance[IO](
    Client.fromHttpApp(
       Routing.instance[IO](
        exec
      )
    )
  )

  test("Build image - success") {

    client
      .build(goodBuild)
      .compile
      .toList
      .map(
        assert.eql(_, List(OutputEvent.Result(goodBuildResult)))
      )
  }



  test("Run hash - unexpected error") {
    client
      .run(unexpectedFailingHash)
      .attempt
      .map(result => assert(result == GenericServerError("server failed").asLeft))
  }

  test("Build image - unknown base error") {

    client
      .build(unknownBaseBuild)
      .compile
      .drain
      .attempt
      .map(result => assert(result == List(OutputEvent.Failure(GenericServerError("Response stream error")))))
  }

  test("Build image - unexpected error") {


    client
      .build(unknownBaseBuild)
      .compile
      .toList
      .map{ result =>
        assert.eql(result, List(OutputEvent.Failure(GenericServerError("Response stream error"))))

      }
  }

  test("Run Hash - success"){
    client.run(goodHash).map(assert.eql(_, goodRunResult))
  }

  test("List images - success"){
    client.listImages.map(assert.eql(_, List(goodHash)))
  }

  // TODO

}