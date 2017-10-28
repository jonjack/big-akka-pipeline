package uk.co.britishgas.batch.oam

import spray.json._

object Model {

  object Brands {
    sealed trait Brand
    case object BG extends Brand
    case object SE extends Brand
    val brands = Seq(BG, SE)
  }

  import Brands._

  case class Customer (title: String, firstName: String, surname: String,
                       email: String, brands: Set[String], status: String, channel: String)

  /*
   * JSON API stupidly has a field called "type" which is reserved in many languages.
   * Use backticks to name a field with a reserved word ie. `type`
   */
  case class JsonApiData (id: String, `type`: String, attributes: Customer)
  case class JsonApiWrapper (data: JsonApiData)

  object CustomerProtocol extends DefaultJsonProtocol {
    implicit val printer = PrettyPrinter
    implicit val custFormat = jsonFormat7(Customer)
  }

  import CustomerProtocol._

  object JsonApiDataProtocol extends DefaultJsonProtocol {
    implicit val printer = PrettyPrinter
    implicit val jadFormat = jsonFormat3(JsonApiData)
  }

  import JsonApiDataProtocol._

  object JsonApiWrapperProtocol extends DefaultJsonProtocol {
    implicit val printer = PrettyPrinter
    implicit val jawFormat = jsonFormat1(JsonApiWrapper)
  }

  import JsonApiWrapperProtocol._

  val cust1 = Customer("Ms", "Tera", "Patrick", "tera@hotmail.com", Set("BG"), "Active", "POT3")
  val cust2 = Customer("Ms", "Tera", "Patrick", "tera@hotmail.com", Set("BG", "SE"), "Active", "POT3")
  val cust1json = cust1.toJson
  val cust2json = cust2.toJson

  //val jad = JsonApiData("00300535746106", "users", cust1)
  val jaw = JsonApiWrapper(JsonApiData("00300535746106", "users", cust2))
  val jawjson = jaw.toJson


  //val c1 = Customer("Ms", "Tera", "Patrick", "tera@hotmail.com", Set("BG"), "Active", "POT3").toJson

  case class Test(mandatory: String, var optional: String)

  val t1 = Test("Mandatory", "Optional")
  //val t2 = Test("Mandatory")

  object TestJsonProtocol extends DefaultJsonProtocol {
    implicit val testFormat = jsonFormat2(Test)
  }

  //val t1j: JsValue = t1.toJson
  //val t2j = t2.toJson

}

/*
import model.OAMModel._
import model.OAMModel.Brands._
import model.OAMModel.Customer
val cust = Customer("Ms", "Tera", "Patrick", "tera@hotmail.com", Set(BG), "POT3", "Active")
val dat = Data(cust)

scala> val cust = Customer("Ms", "Tera", "Patrick", "tera@hotmail.com", Set(BG,SE,BG,SE,SE,BG), "POT3", "Active")
cust: model.OAMModel.Customer = Customer(Ms,Tera,Patrick,tera@hotmail.com,Set(BG, SE),POT3,Active)
*/
