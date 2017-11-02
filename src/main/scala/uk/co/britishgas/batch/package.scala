package uk.co.britishgas

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.{ModeledCustomHeader, ModeledCustomHeaderCompanion}
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

/**
  * Values that are global to all batch components.
  */
package object batch {

  implicit val system: ActorSystem = ActorSystem("batch")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  private val tZero: Long = System.currentTimeMillis()

  private val config: Config = ConfigFactory.load()

  def conf(s: String): String = config.getString(s)

  def printg(s: Any): Unit = println(Console.GREEN + s + Console.RESET)
  def printr(s: Any): Unit = println(Console.RED + s + Console.RESET)

  def timedFuture[T](name: String)(f: Future[T])(implicit ec: ExecutionContext): Future[T] = {
    val start: Long = System.currentTimeMillis()
    printg(s"--> started $name at t0 + ${start - tZero}")
    f.andThen{
      case Success(t) =>
        val end: Long = System.currentTimeMillis()
        printg(s"\t<-- finished $name after ${end - start} millis")
      case Failure(ex) =>
        val end: Long = System.currentTimeMillis()
        printg(s"\t<X> failed $name, total time elapsed: ${end - start}\n$ex")
    }
  }

  def clearOutputDir(): Unit =
    for {
      files <- Option(new File("res").listFiles)
      file <- files if file.getName.endsWith(".csv")
    } file.delete()

  /* JSON API wrapper classes. */
  case class JsonApiData[T] (id: String, `type`: String, attributes: T)
  case class JsonApiRoot[T] (data: JsonApiData[T])

  final class ClientIdHeader(token: String) extends ModeledCustomHeader[ClientIdHeader] {
    override def renderInRequests = true
    override def renderInResponses = false
    override val companion: ClientIdHeader.type = ClientIdHeader
    override def value: String = token
  }
  object ClientIdHeader extends ModeledCustomHeaderCompanion[ClientIdHeader] {
    override val name = "X-Client-ID"
    override def parse(value: String) = Try(new ClientIdHeader(value))
  }
}
