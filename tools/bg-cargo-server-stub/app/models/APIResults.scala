package models.response

import play.api.mvc._
import play.api.mvc.Results._
import play.api.http._

trait APIResults extends Results {
    
    import play.api.http.Status._
    
    class APIStatus(status: Int) extends Status(status) {
        
        val JSON_API_MIME_TYPE = "application/vnd.api+json"
    
        /**
         * Overrides the default result generator to set the MIME type of the Response 
         * Content-Type header to jsonapi.org's MIME type "application/vnd.api+json".
         * 
         * This just adds the convenience of not having to do this for any JSON result 
         * that needs to conform to the jsonapi.org specification.
         * 
         */
        override def apply[C](content: C)(implicit writeable: Writeable[C]): Result = {
          Result(header, writeable.toEntity(content)).as(JSON_API_MIME_TYPE)
        }
        
    }
    
}