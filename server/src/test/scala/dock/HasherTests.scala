package dock

import weaver.*
import cats.effect.IO

object HasherTests extends SimpleIOSuite {

  val hasher = Hasher.sha256Hasher[IO]

  test("empty hash") {
    hasher.hash(SystemState.empty).map { result =>

      assert(result.toHex == "")
    }
  }

  test("hash with one element") {
    hasher.hash(SystemState.empty.upsert("1", "a").map { result =>
      assert(result.toHex == "")
    })
  }

  test("hash with 2 elements") {
    hasher.hash(SystemState.empty.upsert("1", "a").upsert(2, "b")).map { result =>
      assert(result.toHex == "")
    }
  }


  test("hash: insert, delete is empty hash") {
    hasher.hash(SystemState.empty.upsert("1", "a").delete(1)).map { result =>
      assert(result.toHex == "")
    }
  }
}
