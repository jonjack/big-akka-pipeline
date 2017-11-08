package uk.co.britishgas.streams.oam

import akka.event.{Logging, LoggingAdapter}
import spray.json._
import uk.co.britishgas.streams._
import uk.co.britishgas.streams.oam.Model.Customer

import scala.util.{Failure, Success, Try}

object Marshallers {

  private val logFailure: LoggingAdapter = Logging.getLogger(system, "failure")

  object CustomerJsonProtocol {
    import DefaultJsonProtocol._
    implicit val printer: PrettyPrinter.type = PrettyPrinter
    implicit val customerFormat: JsonFormat[Customer] = jsonFormat7(Customer)
    implicit val dataFormat: JsonFormat[JsonApiData[Customer]] = jsonFormat3(JsonApiData[Customer])
    implicit val rootFormat: RootJsonFormat[JsonApiRoot[Customer]] = jsonFormat1(JsonApiRoot[Customer])
  }

  private def marshalCustomer(in: String): Try[String] = {
    import CustomerJsonProtocol._
    Try {
      val inp: Array[String] = in.trim.split("\\|")
      val brands: Set[String] = inp(5).trim.split(",").toSet
      val cust: Customer = Customer(inp(1), inp(2), inp(3), inp(4), brands, inp(6), inp(7))
      val data: JsonApiData[Customer] = JsonApiData(inp(0), "users", cust)
      val root: JsonApiRoot[Customer] = JsonApiRoot(data)
      root.toJson.compactPrint
    }
  }

  def buildJson(in: String): Option[String] = {
    marshalCustomer(in) match {
      case Success(json) => Option(json)
      case Failure(ex) => {
        logFailure.info(s"JSON Marshalling failed because input[$in] caused: ${ex}")
        Option(null)
      }
    }
  }

}
