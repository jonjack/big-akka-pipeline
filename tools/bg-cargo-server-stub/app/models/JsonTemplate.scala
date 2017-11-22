package models.response

import play.api.libs.json._

/**
 * Global wrapper for all Json Response types.
 * 
 * <TODO> - revisit this at some point to consider if we wish to rework it
 * to comply with the standard defined @ http://jsonapi.org/
 */
case class JsonTemplate (
  
    //val status:           JsValue,
    val data:             JsValue,
    val errors:           JsValue
    //val state:            JsValue
    
)
{
    implicit val jsonResultReads = Json.reads[JsonTemplate]
    implicit val jsonResultWrites = Json.writes[JsonTemplate]
    implicit val jsonResultFormat = Json.format[JsonTemplate]  
    
    val asJson: JsValue = Json.toJson(this)
    val asAsciiString = Json.asciiStringify(this.asJson)    // escapes non-ascii characters, see http://timelessrepo.com/json-isnt-a-javascript-subset
    val asPrettyJson = Json.prettyPrint(asJson)
    
}

object JsonTemplate {}

trait jsonapi {
    
   // case class root
    
}

/**
 * jsonapi.org doc structure for multiple resource objects eg. 'entries'

{
	"data": [
		{ entry1 },
		{ entry1 },
		{ entry1 }
	]
}
*/

/**
 * Example jsonapi.org doc
{
	"data": [
		{
		    "id": "17",
		    "type": "entry",
		    "attributes": {
		        "caught": "2016-04-26T08:46:27Z",
		        "created": "2016-04-26T08:46:27Z",
		        "conditions": "sunny",
		        "tackle": "klinkhammer, 3 weight 8 foot rod",
		        "bait": "dead sprat",
		        "notes": "flood water",
		        "species": "pike",
		        "length": "12",
		        "lengthUnits": "inches",
		        "weight": "20",
		        "weightUnits": "ounces",
		        "image": {
		            "links": {
		                "self": "http://localhost/images/1204"
		            }
		        },
		        "location": {
		            "id": "35",
		            "type": "location",
		            "attributes": {
		                "country": "GB",
		                "water": "river",
		                "name": "Colne",
		                "position": {
		                    "latitude": "51.045762",
		                    "longitude": "-1.503021"
		                }
		            }
		            
		        }
		    },
		    "relationships": {
		        "owner": {
		            "data": {
		                "id": "45", 
		                "type": "member"
		            }
		        }
		    }
		 },
		{
		    "id": "57",
		    "type": "entry",
		    "attributes": {
		        "caught": "2016-05-01T09:23:20Z",
		        "created": "2016-04-01T09:23:20Z",
		        "conditions": "light cloud",
		        "tackle": "shakespear biscay fly rod",
		        "bait": "red hawkhead fly",
		        "notes": "This fly working very well today at this location.",
		        "species": "trout",
		        "length": "11",
		        "lengthUnits": "inches",
		        "weight": "15",
		        "weightUnits": "ounces",
		        "image": {
		            "links": {
		                "self": "http://localhost/images/806"
		            }
		        },
		        "location": {
		            "id": "61",
		            "type": "location",
		            "attributes": {
		                "country": "GB",
		                "water": "stillWater",
		                "name": "Carney Pool",
		                "position": {
		                    "latitude": "51.032762",
		                    "longitude": "-1.601021"
		                }
		            }
		            
		        }
		    },
		    "relationships": {
		        "owner": {
		            "data": {
		                "id": "109", 
		                "type": "member"
		            }
		        }
		    }
		 }
	]
}
*/
