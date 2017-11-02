package uk.co.britishgas.batch.oam

import java.nio.file.{Path, Paths}

import akka.stream.scaladsl.{FileIO, Flow, Framing, Sink, Source}
import akka.stream.{IOResult, ThrottleMode}
import akka.util.ByteString
import spray.json._
import uk.co.britishgas.batch.oam.Marshallers._
import uk.co.britishgas.batch.oam.Model._
import uk.co.britishgas.batch.{conf, system, _}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object TestMarshaller extends App {

  private val file: String = conf("source-dir") + "/" + conf("source-file")
  private val path: Path = Paths.get(file)
  private val throttleRate: Int = conf("throttle-rate").toInt
  private val throttleBurst: Int = conf("throttle-burst").toInt

  //        0      1   2      3             3               5    6     7
  // val in = "003005400124|Ms|Tera|Patrick|tera.patrick@hotmail.com|BG,SE|active|PPOT3"

  /*
   * Takes an input String with pattern:
   * "003005400124|Ms|Tera|Patrick|tera.patrick@hotmail.com|BG,SE|active|PPOT3"
   * and attempts to build a JSON Object out of it.
   * Will return a Failure where an Exception was thrown.
   */
  private def marshalCustomer(in: String): Try[String] = {
    import CustomerJsonProtocol._
    Try {
      val inp: Array[String] = in.trim.split("\\|")
      val cust: Customer = Customer(inp(1), inp(2), inp(3), inp(4), Set[String](inp(5)), inp(6), inp(7))
      val data: JsonApiData[Customer] = JsonApiData(inp(0), "users", cust)
      val root: JsonApiRoot[Customer] = JsonApiRoot(data)
      //root.toJson.compactPrint    //
      root.toJson.prettyPrint
    }
  }

  def buildJson(in: String): Option[String] = {
      marshalCustomer(in) match {
        case Success(json) => Option(json)   // println(json)
        case Failure(ex) => Option(null)     // println(s"JSON Marshalling failed because input[$in] caused: ${ex}")
      }
  }

  val in = "003005400124|Ms|Tera|Patrick|tera.patrick@hotmail.com|BG,SE|active|PPOT3"
  val resp = marshalCustomer(in)


  val st = "003005400124|Ms|Tera|Patrick|tera.patrick@hotmail.com|BG|active|PPOT3"

  val source: Source[ByteString, Future[IOResult]] = FileIO.fromPath(path)

//  val throttle = Flow[ByteString].
//    via(Framing.delimiter(ByteString(System.lineSeparator), 10000)).
//    throttle(throttleRate, 1.second, throttleBurst, ThrottleMode.shaping).
//    map((bs: ByteString) => { bs.utf8String })

  // buildJson(in).get.parseJson.asJsObject.fields("data").asJsObject.fields("id").toString
  private def extractId(jst: String) = jst.parseJson.asJsObject.fields("data").asJsObject.fields("id").toString

  val throttle = Flow[ByteString].
    via(Framing.delimiter(ByteString(System.lineSeparator), 10000)).
    throttle(throttleRate, 1.second, throttleBurst, ThrottleMode.shaping).
    map((bs: ByteString) => { buildJson(bs.utf8String) }).
    filter((op: Option[String]) => { op.nonEmpty }).
    map((op: Option[String]) => {
      val json: String = op.get
      val id: String = extractId(json)
      (id, json) // emit the tuple downstream
    })


  // val op = buildJson(in).get.parseJson.asJsObject.fields.get("data").get.asJsObject.fields.get("id").get.toString

  val sink = Sink.foreach(println)

  val graph: Unit = source.via(throttle).runWith(sink).onComplete(_ => system.terminate())

//  case class Customer(
//                       `first-name`: String,
//                       title: String,
//                       surname: String,
//                       email: String,
//                       brands: Set[String],
//                       status: String,
//                       channel: String)
  // case class JsonApiData[T] (id: String, `type`: String, attributes: T)
  // case class JsonApiRoot[T] (data: JsonApiData[T])
  // We need to marshall a String that looks like this:
  // 003005400124|Ms|Tera|Patrick|tera.patrick@hotmail.com|BG|active|PPOT3
  // Into This
  // {

//  "data": {
//    "id": "003610070499",
//    "type": "users",
//    "attributes": {
//    "channel": "PPOT3",
//    "email": "harry42callahan45@gmail.com",
//    "surname": "Callahan",
//    "status": "active",
//    "brands": ["BG"],
//    "first-name": "Harry",
//    "title": "Mr"
//  }
//  }
//}
}
