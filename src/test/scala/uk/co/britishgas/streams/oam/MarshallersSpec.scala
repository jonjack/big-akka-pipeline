package uk.co.britishgas.streams.oam

import org.scalatest.{FlatSpecLike, Matchers}
import spray.json._
import uk.co.britishgas.streams.oam.Model._
import uk.co.britishgas.streams._

class MarshallersSpec extends FlatSpecLike with Matchers {

  import Marshallers.CustomerJsonProtocol._

  private val marshalledCustomer: String =
    """{"channel":"CRM","email":"terapatrick@gmail.com","surname":"Patrick","status":"active",
      |"brands":["BG"],"first-name":"Tera","title":"Ms"}""".stripMargin.replaceAll("\n", "")

  private val marshalledJsonApiCustomer: String =
    """{"data":{"id":"003005400001","type":"users","attributes":{"channel":"CRM",
      |"email":"terapatrick@gmail.com","surname":"Patrick","status":"active",
      |"brands":["BG"],"first-name":"Tera","title":"Ms"}}}""".stripMargin.replaceAll("\n", "")

  val customer1: Customer = Customer("Ms", "Tera", "Patrick", "terapatrick@gmail.com", Set("BG"), "active", "CRM")

  "Marshalling a Customer object to compact JSON" should "return a valid JSON string" in {
    val json: String = customer1.toJson.compactPrint
    assert(json == marshalledCustomer)
  }

  it should "return a valid compact JSON API format string" in {
    val json: JsValue = customer1.toJson
    val japid: JsonApiData[Customer] = JsonApiData("003005400001", "users", customer1)
    val japi: JsonApiRoot[Customer] = JsonApiRoot(japid)
    val japijson: String = japi.toJson.compactPrint
    assert(japijson == marshalledJsonApiCustomer)
  }

  private val prettyMarshalledCustomer: String = """{""" + "\n" +
    """  "channel": "AWB",""" + "\n" +
    """  "email": "harrycallahan@gmail.com",""" + "\n" +
    """  "surname": "Callahan",""" + "\n" +
    """  "status": "active",""" + "\n" +
    """  "brands": ["SE"],""" + "\n" +
    """  "first-name": "Harry",""" + "\n" +
    """  "title": "Mr"""" + "\n" +
    """}"""

  private val prettyMarshalledJsonApiCustomer: String = """{""" + "\n" +
    """  "data": {""" + "\n" +
    """    "id": "003005400002",""" + "\n" +
    """    "type": "users",""" + "\n" +
    """    "attributes": {""" + "\n" +
    """      "channel": "AWB",""" + "\n" +
    """      "email": "harrycallahan@gmail.com",""" + "\n" +
    """      "surname": "Callahan",""" + "\n" +
    """      "status": "active",""" + "\n" +
    """      "brands": ["SE"],""" + "\n" +
    """      "first-name": "Harry",""" + "\n" +
    """      "title": "Mr"""" + "\n" +
    """    }""" + "\n" +
    """  }""" + "\n" +
    """}"""

  val customer2: Customer = Customer("Mr", "Harry", "Callahan", "harrycallahan@gmail.com", Set("SE"), "active", "AWB")

  "Marshalling a Customer object to pretty JSON" should "return a valid pretty JSON string" in {
    val japijson: String = customer2.toJson.prettyPrint
    assert(japijson == prettyMarshalledCustomer)
  }

  it should "return a valid pretty JSON API format" in {
    val json: JsValue = customer2.toJson
    val japid: JsonApiData[Customer] = JsonApiData("003005400002", "users", customer2)
    val japi: JsonApiRoot[Customer] = JsonApiRoot(japid)
    val japijson: String = japi.toJson.prettyPrint
    assert(japijson == prettyMarshalledJsonApiCustomer)
  }

}
