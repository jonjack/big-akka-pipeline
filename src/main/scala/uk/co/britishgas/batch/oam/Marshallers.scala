package uk.co.britishgas.batch.oam

import akka.event.{Logging, LoggingAdapter}
import spray.json._
import uk.co.britishgas.batch.oam.Model._
import uk.co.britishgas.batch.{JsonApiData, JsonApiRoot, system}

import scala.util.{Failure, Success, Try}

object Marshallers {

  private val logfailure: LoggingAdapter = Logging.getLogger(system, "failure")

  object CustomerJsonProtocol {
    import DefaultJsonProtocol._
    implicit val printer: PrettyPrinter.type = PrettyPrinter
    implicit val customerFormat: JsonFormat[Customer] = jsonFormat7(Customer)
    implicit val dataFormat: JsonFormat[JsonApiData[Customer]] = jsonFormat3(JsonApiData[Customer])
    implicit val rootFormat: RootJsonFormat[JsonApiRoot[Customer]] = jsonFormat1(JsonApiRoot[Customer])
  }

  /**
   * Takes an input String with schema:
   * "003005400124|Ms|Tera|Patrick|tera.patrick@hotmail.com|BG,SE|active|PPOT3"
   * and attempts to build a JSON Object out of it.
   * Will return a Failure where an Exception was thrown.
   */
  private def marshalCustomer(in: String): Try[String] = {
    import CustomerJsonProtocol._
    Try {
      val inp: Array[String] = in.trim.split("\\|")
      val brands: Set[String] = inp(5).trim.split(",").toSet
      val cust: Customer = Customer(inp(1), inp(2), inp(3), inp(4), brands, inp(6), inp(7))
      val data: JsonApiData[Customer] = JsonApiData(inp(0), "users", cust)
      val root: JsonApiRoot[Customer] = JsonApiRoot(data)
      //root.toJson.prettyPrint
      root.toJson.compactPrint
    }
  }

  def buildJson(in: String): Option[String] = {
    marshalCustomer(in) match {
      case Success(json) => Option(json)
      case Failure(ex) => {
        logfailure.info(s"JSON Marshalling failed because input[$in] caused: ${ex}")
        Option(null)
      }
    }
  }

}
