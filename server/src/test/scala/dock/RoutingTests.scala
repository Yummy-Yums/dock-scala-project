package dock

import munit.CatsEffectSuite
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.client.dsl.io.*
import cats.implicits.*
import org.http4s.client.Client
import org.http4s.implicits.uri
import weaver.SimpleIOSuite

object RoutingTests extends SimpleIOSuite {

  val exec = Client.fromHttpApp(
    Routing.instance(
      TestExecutor.instance(
        Map.empty,
        Map(Hash(Vector(40, 100)) -> TestResult.Success(SystemState(Map("K" -> "V"))))
      )
    )
  )

  test("PUT /api/run") {
    val body = io.circe
        .parser
        .parse("""
         {
            "value": [40, 100]
         }
         """).toOption
             .get

      val output =
        io.circe
          .parser
          .parse(
        """
           {
            "all": {
              "K": "V"
            }
           }
        """).toOption
            .get


      exec
        .expect[Json](
        POST(input, uri"/api/run")
        )
        .map {
          assert.eql(_, output)
        }
        output

  }
}
