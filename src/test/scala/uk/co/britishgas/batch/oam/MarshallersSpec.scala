package uk.co.britishgas.batch.oam

import org.scalatest.{FlatSpecLike, Matchers}
import spray.json._
import uk.co.britishgas.batch.oam.Model._
import uk.co.britishgas.batch.{JsonApiData, JsonApiRoot, system}

import uk.co.britishgas.batch._
import uk.co.britishgas.batch.oam.Marshallers._

class MarshallersSpec extends FlatSpecLike with Matchers {

  // TODO - Move to test package
  // A test of the implicit marshallers
  /*
  val cust: Customer = Customer("Ms", "Tera", "Patrick", "terapatrick@gmail.com", Set("BG"), "active", "CRM")
  val json: JsValue = cust.toJson
  val jad: JsonApiData[Customer] = JsonApiData("003610070499", "users", cust)
  val jaw: JsonApiRoot[Customer] = JsonApiRoot(jad)
  val jawjson: String = jaw.toJson.prettyPrint
  // NOTE: I ran the result of generating jawjson in Postman against /users in Test (Dig 01) and it was successful
*/

  //  note - need cpmpact versions of following as well - testing the pretty format is not that useful since it
  // wont be sent across the wire this way for compactness.

  val marshalledCustomer: String = """{""" + "\n" +
    """  "data": {""" + "\n" +
    """    "id": "003610070499",""" + "\n" +
    """    "type": "users",""" + "\n" +
    """    "attributes": {""" + "\n" +
    """      "channel": "CRM",""" + "\n" +
    """      "email": "terapatrick@gmail.com",""" + "\n" +
    """      "surname": "Patrick",""" + "\n" +
    """      "status": "active",""" + "\n" +
    """      "brands": ["BG"],""" + "\n" +
    """      "first-name": "Tera",""" + "\n" +
    """      "title": "Ms"""" + "\n" +
    """    }""" + "\n" +
    """  }""" + "\n" +
    """}"""

  val marshalledJSONAPICustomer: String = """{""" + "\n" +
    """  "data": {""" + "\n" +
    """    "id": "003610070499",""" + "\n" +
    """    "type": "users",""" + "\n" +
    """    "attributes": {""" + "\n" +
    """      "channel": "CRM",""" + "\n" +
    """      "email": "terapatrick@gmail.com",""" + "\n" +
    """      "surname": "Patrick",""" + "\n" +
    """      "status": "active",""" + "\n" +
    """      "brands": ["BG"],""" + "\n" +
    """      "first-name": "Tera",""" + "\n" +
    """      "title": "Ms"""" + "\n" +
    """    }""" + "\n" +
    """  }""" + "\n" +
    """}"""

//  "Marshalling a Customer object to JSON" should "return a valid JSON string" in {
//    val cust: Customer = Customer("Ms", "Tera", "Patrick", "terapatrick@gmail.com", Set("BG"), "active", "CRM")
//    val json: JsValue = cust.toJson
//    val jad: JsonApiData[Customer] = JsonApiData("003610070499", "users", cust)
//    val jaw: JsonApiRoot[Customer] = JsonApiRoot(jad)
//    val jawjson: String = jaw.toJson.prettyPrint
//    assert(evt !== null)
//  }


  import Marshallers.CustomerJsonProtocol._


  "Marshalling a JSON API Customer object to JSON" should "return a valid JSON string" in {
    val cust: Customer = Customer("Ms", "Tera", "Patrick", "terapatrick@gmail.com", Set("BG"), "active", "CRM")
    val json: JsValue = cust.toJson
    val japid: JsonApiData[Customer] = JsonApiData("003610070499", "users", cust)
    val japi: JsonApiRoot[Customer] = JsonApiRoot(japid)
    //val japijson: String = japi.toJson.compactPrint
    val japijson: String = japi.toJson.prettyPrint
    assert(japijson == marshalledJSONAPICustomer)
  }

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

}
