package uk.co.britishgas.streams.oam

import akka.event.{Logging, LoggingAdapter}
import spray.json._
import uk.co.britishgas.streams._
import uk.co.britishgas.streams.oam.Model.Customer

import scala.util.{Failure, Success, Try}

object Marshallers {

  private val logFailure: LoggingAdapter = Logging.getLogger(system, "failure")

  case class InvalidInputException(message: String) extends Exception(message)

  object CustomerJsonProtocol {
    import DefaultJsonProtocol._
    implicit val printer: PrettyPrinter.type = PrettyPrinter
    implicit val customerFormat: JsonFormat[Customer] = jsonFormat7(Customer)
    implicit val dataFormat: JsonFormat[JsonApiData[Customer]] = jsonFormat3(JsonApiData[Customer])
    implicit val rootFormat: RootJsonFormat[JsonApiRoot[Customer]] = jsonFormat1(JsonApiRoot[Customer])
  }
  
  private def checkInputArray(arr: Array[String]): Boolean =
    if(arr.length >= 7) true
    else throw InvalidInputException("Input array was below mandatory length (>= 7 fields)")

  private def extractToken(token: String, id: String): String =
    if(token.trim.nonEmpty) token
    else throw InvalidInputException(s"$id was empty")

  private def extractBrands(bds: String): Set[String] = {
    val rawBrands: String = extractToken(bds, "brands")
    val brands: Array[String] = rawBrands.trim.split(",").map((token: String) => token.trim.toUpperCase).
      filter((br: String) => br.equals("BG") || br.equals("SE"))
    if(brands.nonEmpty) brands.toSet
    else throw InvalidInputException("No valid brand(s) supplied.")
  }

  private def marshalCustomer(in: String): Try[String] = {
    import CustomerJsonProtocol._
    Try {
      val input: Array[String] = in.trim.split("\\|")
      checkInputArray(input)
      val ucrn: String = extractToken(input(0), "ucrn")
      val title: String = extractToken(input(1), "title")
      val firstname: String = extractToken(input(2), "firstname")
      val surname: String = extractToken(input(3), "surname")
      val email: String = extractToken(input(4), "email")
      val brands: Set[String] = extractBrands(input(5))
      val status: String = extractToken(input(6), "status")
      val channel: String = extractToken(input(7), "channel")
      val cust: Customer = Customer(title, firstname, surname, email, brands, status, channel)
      val data: JsonApiData[Customer] = JsonApiData(ucrn, "users", cust)
      val root: JsonApiRoot[Customer] = JsonApiRoot(data)
      root.toJson.compactPrint
    }
  }

  def buildJson(in: String): Option[String] = {
    marshalCustomer(in) match {
      case Success(json) => Option(json)
      case Failure(ex) => {
        logFailure.error(s"JSON Marshalling failed because input[$in] caused: ${ex}")
        Option(null)
      }
    }
  }

}
