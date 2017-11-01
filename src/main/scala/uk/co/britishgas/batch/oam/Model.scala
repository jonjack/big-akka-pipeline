package uk.co.britishgas.batch.oam

import spray.json._

object Model {

  object Brands {
    sealed trait Brand
    case object BG extends Brand
    case object SE extends Brand
    val brands = Seq(BG, SE)
  }

  case class Customer(title: String, `first-name`: String, surname: String, email: String,
                       brands: Set[String], status: String, channel: String)

  case class JsonApiData (id: String, `type`: String = "users", attributes: Customer)
  case class JsonApiWrapper (data: JsonApiData)

  object OAMProtocol {
    import DefaultJsonProtocol._
    implicit val printer = PrettyPrinter
    implicit val custFormat: JsonFormat[Customer] = jsonFormat7(Customer)
    implicit val jadFormat: JsonFormat[JsonApiData] = jsonFormat3(JsonApiData)
    implicit val jawFormat: RootJsonFormat[JsonApiWrapper] = jsonFormat1(JsonApiWrapper)
  }

  import DefaultJsonProtocol._
  import OAMProtocol._

  val cust = Customer("Mr", "Harry", "Callahan", "harry42callahan.com", Set("BG"), "active", "PPOT3")
  val json = cust.toJson

  val jad = JsonApiData("003610070899", "users", cust)
  val jaw = JsonApiWrapper(jad)
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
import model.OAMModel._
import model.OAMModel.Brands._
import model.OAMModel.Customer
val cust = Customer("Ms", "Tera", "Patrick", "tera@hotmail.com", Set(BG), "POT3", "Active")
val dat = Data(cust)

scala> val cust = Customer("Ms", "Tera", "Patrick", "tera@hotmail.com", Set(BG,SE,BG,SE,SE,BG), "POT3", "Active")
cust: model.OAMModel.Customer = Customer(Ms,Tera,Patrick,tera@hotmail.com,Set(BG, SE),POT3,Active)
*/
