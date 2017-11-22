        package models.response

import play.api.libs.json._

abstract class Json[T] {
    
    /**
    implicit val jsonResultReads = Json.reads[T]
    implicit val jsonResultWrites = Json.writes[T]
    implicit val jsonResultFormat = Json.format[T] 
     
    val asJson: JsValue = Json.toJson(this)
    val asAsciiString = Json.asciiStringify(this.asJson)    // escapes non-ascii characters, see http://timelessrepo.com/json-isnt-a-javascript-subset
    val asPrettyJson = Json.prettyPrint(asJson)
    */
}