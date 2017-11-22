package models.response

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import java.time.ZonedDateTime

trait Test

case class Entry(id: Int, typ: String, caught: ZonedDateTime)
case class Data(entries: Seq[Entry], someStr: String)
case class Root(data: Seq[Entry])

case class Location(lat: Double, long: Double)
case class Resident(name: String, age: Int, role: Option[String])
case class Place(name: String, location: Location, residents: Seq[Resident])


case class Prop(data: Seq[Entry])
case class Sub(name: String)


object Test {
    
    /*
    implicit val subReads: Reads[Sub] = (
      (__ \ "name").read[String] and
      (__ \ "age").read[String]   
    )(Sub.apply _)
    *
    */
    
    implicit val subReads: Reads[Sub] = (
        (__ \ "name").read[String].map(Sub(_))
    )

    /*
    implicit val propReads: Reads[Prop] = (
      (__ \ "prop1").read[String] and
      (__ \ "prop2").read[Seq[Sub]]   
    )(Prop.apply _)
		*/

    implicit val entryReads: Reads[Entry] = (
      (__ \ "id").read[Int] and
      (__ \ "typ").read[String] and
      (__ \ "caught").read[ZonedDateTime]
    )(Entry.apply _)
    
    
        
    implicit val propReads: Reads[Prop] = (
        (__ \ "data").read[Seq[Entry]].map(Prop(_))
    )
    
    implicit val rootReads: Reads[Root] = (
        (__ \ "data").read[Seq[Entry]].map(Root)
    )
    
    
    /*
    implicit val propWrites: Writes[Prop] = (
        (__ \ "data").write[Seq[Entry]].contramap(prop => prop.)
    )
    */
    
    
    //implicit val dataReads: Reads[Data] = (JsPath \ "entries").read[Seq[Entry]]
    
    //implicit val rootReads: Reads[Root] = (JsPath \ "seq").read[Root]

    
    
    implicit val dataReads: Reads[Data] = (
      (__ \ "entries").read[Seq[Entry]] and
      (__ \ "someStr").read[String]
    )(Data.apply _)
    
    
    
    //implicit val dataReads: Reads[Data] = (JsPath \ "seq").read[Seq[Entry]]
    
    //val secReads: Reads[Seq[Entry]] = (JsPath \ "seq").read[Seq[Entry]]
    
    //val dataReadsBuilder = (JsPath \ "seq").read[Seq[Entry]]
   // implicit val dataReads = dataReadsBuilder.apply(secReads)
    
    
    //implicit val secReads: Reads[Seq[Entry]] = (JsPath \ "seq").read[Seq[Entry]]
    //implicit val dataReads: Reads[Data] = (JsPath \ "seq").read[Data]
    
    /*
    implicit val rootReads: Reads[Root] = (
        (JsPath \ "data").read[Data] and
        (JsPath \ "none").readNullable[String]
    )(Root.apply _)

    
    implicit val rootReads: Reads[Root] = (
      (JsPath \ "data").read[Data] and
      (JsPath \ "none").readNullable[String]
    )(Root.apply _)
    * 
    *
    *
    */
    

  
    
    /*
        
    implicit val rootReads: Reads[Root] = (
      (JsPath \ "data").read[Data]
    )(Root.apply _)
    */
    /*
    implicit val dataReads: Reads[Data] = (
      (JsPath \ "seq").read[String]
    )(Data.apply _)
*/
    
    
        implicit val locationReads: Reads[Location] = (
      (JsPath \ "lat").read[Double] and
      (JsPath \ "long").read[Double]
    )(Location.apply _)
    
    implicit val residentReads: Reads[Resident] = (
      (JsPath \ "name").read[String] and
      (JsPath \ "age").read[Int] and
      (JsPath \ "role").readNullable[String]
    )(Resident.apply _)
    
    implicit val placeReads: Reads[Place] = (
      (JsPath \ "name").read[String] and
      (JsPath \ "location").read[Location] and
      (JsPath \ "residents").read[Seq[Resident]]
    )(Place.apply _)
    
    implicit val locationWrites: Writes[Location] = (
      (JsPath \ "lat").write[Double] and
      (JsPath \ "long").write[Double]
    )(unlift(Location.unapply))
    
    implicit val residentWrites: Writes[Resident] = (
      (JsPath \ "name").write[String] and
      (JsPath \ "age").write[Int] and
      (JsPath \ "role").writeNullable[String]
    )(unlift(Resident.unapply))
    
    implicit val placeWrites: Writes[Place] = (
      (JsPath \ "name").write[String] and
      (JsPath \ "location").write[Location] and
      (JsPath \ "residents").write[Seq[Resident]]
    )(unlift(Place.unapply))
   
}
    
    
    