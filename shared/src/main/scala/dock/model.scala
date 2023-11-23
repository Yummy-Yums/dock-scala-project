//package dock
//
//import io.circe.*
//import io.circe.generic.auto.*
//import sttp.tapir.Schema
//
//enum Command {
//  case Build(build: dock.Build)
//  case Run(hash: Hash)
//} 
//
//final case class Build(
//     base: Build.Base,
//     commands: List[Build.Command]
//) derives Codec.AsObject, Schema
//
//
//object Build {
//
//  enum Base derives Codec.AsObject {
//    case EmptyImage
//    case ImageHash(hash: Hash)
//  }
//
//  enum Command derives Codec.AsObject {
//    case Upsert(key: String, value: String)
//    case Delete(key: String)
//  }
//
//  val empty: Build = Build(Base.EmptyImage, Nil)
//}
//
//final case class Hash(value: Vector[Byte]) derives Codec.AsObject, Schema
//
//final case class SystemState(all: Map[String, String]) derives Codec.AsObject, Schema