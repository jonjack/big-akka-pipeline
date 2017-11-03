package uk.co.britishgas.batch.oam

import akka.event.{Logging, LoggingAdapter}
import spray.json._
import uk.co.britishgas.batch.oam.Model._
import uk.co.britishgas.batch.{JsonApiData, JsonApiRoot, system}

import scala.util.{Failure, Success, Try}

object Marshallers {

  private val logfailure: LoggingAdapter = Logging.getLogger(system, "failure")

  // implicit JSON marshallers for the Customer type
  object CustomerJsonProtocol {
    import DefaultJsonProtocol._
    implicit val printer: PrettyPrinter.type = PrettyPrinter
    implicit val customerFormat: JsonFormat[Customer] = jsonFormat7(Customer)
    implicit val dataFormat: JsonFormat[JsonApiData[Customer]] = jsonFormat3(JsonApiData[Customer])
    implicit val rootFormat: RootJsonFormat[JsonApiRoot[Customer]] = jsonFormat1(JsonApiRoot[Customer])
  }

  // TODO - Move to test package
  // A test of the implicit marshallers
  /*
  val cust: Customer = Customer("Mr", "Harry", "Callahan", "harry42callahan45@gmail.com", Set("BG"), "active", "PPOT3")
  val json: JsValue = cust.toJson
  val jad: JsonApiData[Customer] = JsonApiData("003610070499", "users", cust)
  val jaw: JsonApiRoot[Customer] = JsonApiRoot(jad)
  val jawjson: String = jaw.toJson.prettyPrint

  // NOTE: I ran the result of generating jawjson in Postman against /users in Test (Dig 01) and it was successful

  val test: Boolean = jawjson == """{""" + "\n" +
    """  "data": {""" + "\n" +
    """    "id": "003610070499",""" + "\n" +
    """    "type": "users",""" + "\n" +
    """    "attributes": {""" + "\n" +
    """      "channel": "PPOT3",""" + "\n" +
    """      "email": "harry42callahan45@gmail.com",""" + "\n" +
    """      "surname": "Callahan",""" + "\n" +
    """      "status": "active",""" + "\n" +
    """      "brands": ["BG"],""" + "\n" +
    """      "first-name": "Harry",""" + "\n" +
    """      "title": "Mr"""" + "\n" +
    """    }""" + "\n" +
    """  }""" + "\n" +
    """}"""

*/
  /* Sample /users Request body
  {
    "data": {
    "id": "003610070899",
    "type": "users",
    "attributes": {
      "brands": [
        "SE",
        "BG"
       ],
      "title": "Mr",
      "first-name": "Harry",
      "surname": "Callahan",
      "email": "harry42callahan805@gmail.com",
      "status": "active",
      "channel": "PPOT3"
      }
    } // close of `data`
  }
  */

  /*
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
      //root.toJson.compactPrint    //
      root.toJson.prettyPrint
    }
  }

  def buildJson(in: String): Option[String] = {
    println("Attempting to marshal: " + in)
    marshalCustomer(in) match {
      case Success(json) => Option(json)
      case Failure(ex) => {
        logfailure.info(s"JSON Marshalling failed because input[$in] caused: ${ex}")
        Option(null)
      }
    }
  }

}
