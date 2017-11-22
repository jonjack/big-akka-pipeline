package models

import play.api.mvc._
import play.api.http._

object CustomMimeTypes extends CustomMimeTypes
trait CustomMimeTypes extends MimeTypes {
  val JSONAPI = "application/vnd.api+json"
}

trait AcceptHeaderExtractors extends AcceptExtractors {

  object Accept {
    val JsonApi = Accepting(CustomMimeTypes.JSONAPI)
  }

}
