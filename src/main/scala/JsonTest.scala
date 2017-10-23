
import dispatch._
import scala.concurrent.duration._
import scala.concurrent.{Future, ExecutionContext}
import org.json4s.JsonAST.{JValue, JString}
import scala.collection.immutable._

object JsonTest {

  /*
  object LinkListing {
    def fromJson(subreddit: String)(json: JValue) = {
      val x = json.\("data").\("children").children.map(_.\("data").\("id")).collect{ case JString(s) => Link(s, subreddit) }
      LinkListing(x)
    }
  }

}

object LinkListing {
  def fromJson(subreddit: String)(json: JValue) = {
    val x = json.\("data").\("children").children.map(_.\("data").\("id")).collect{ case JString(s) => Link(s, subreddit) }
    LinkListing(x)
  }

  */
}