package dock

import cats.effect.IO
import munit.CatsEffectSuite
import cats.effect.SyncIO

class ServerSideExecutorTest extends CatsEffectSuite {

  val execR = ServerSideExecutor.module[SyncIO]

//  Eq[]

  test("Build empty Image") {

    fs2
      .Stream
      .resource(execR)
      .flatMap(_.build(Build.empty))
      .compile
      .toList
      .map { logs =>
        assert.eql(
          logs.map(_.map(_.map(_.toHex))),
          List(
            OutputEvent.LogMessage("hello world"),
            OutputEvent.LogMessage("goodbye world"),
            OutputEvent.Result(
              Hash.parse(" ")
            )
          ),
        )

      }
  }
  
  test("Build and run empty image") {

      execR
        .use(exec => OutputEvent.getResult(exec.build(Build.empty)).rethrow.flatMap(exec.run))
        .map(_.all)
        .map(
          assert.eql(_, Map.empty)
        )
  }

}
