package uk.co.britishgas.batch.oam

import java.nio.file.{Path, Paths}

import akka.event.{Logging, LoggingAdapter}
import akka.stream.scaladsl.{FileIO, Flow, Framing, Sink, Source}
import akka.stream.{IOResult, ThrottleMode}
import akka.util.ByteString
import uk.co.britishgas.batch._

import scala.concurrent.Future
import scala.concurrent.duration._

/*
 * Batch creator of OAM records.
 *
 * As a batch consumer of the POST operation on the Users API, this
 * Akka HTTP client will take a potentially unbounded Source of
 * Contact Person records and make a HTTP POST Request on /users for
 * each.
 *
 * On completion, this client generates two logs - Success and Failure.
 */
object Client extends App {

  private val log: LoggingAdapter = Logging.getLogger(system, this)
  private val path: String = conf("inbound-dir") + "/" + conf("source")
  private val file: Path = Paths.get(path)
  private val rate: String = conf("api-rate")

  printg("Source: " + file + " Rate: " + rate)

  import scala.concurrent.ExecutionContext.Implicits.global

  val source: Source[ByteString, Future[IOResult]] = FileIO.fromPath(file)

  val flow = Flow[ByteString].
    via(Framing.delimiter(ByteString(System.lineSeparator), 10000)).
    throttle(1, 1.second, 1, ThrottleMode.shaping).
    map(bs => bs.utf8String)

  val sink = Sink.foreach(println)

  /*
   * The secret to getting this to work was to use runWith(sink) rather than to(sink).
   */
  val graph: Unit = source.via(flow).runWith(sink).onComplete(_ => system.terminate())

}


//val flow: Flow[ByteString, Customer, NotUsed] =
//Flow[ByteString]
////.via(Framing.delimiter(ByteString("\n"), 1024))
////.via(Framing.delimiter(ByteString("|"), 1024))
//.map(bs => bs.utf8String.trim.split("|"))
//.map(res =>
//res match {
//case Array(bpid, title, firstname, surname, brand, email, channel) => Customer(bpid, title, firstname, surname, brand, email, channel)
//case _ => Customer("30065400124", "Ms", "Tera", "Patrick", "SE", "tera.patrick@hotmail.com", "POT3")
//})
//
//import MyJsonProtocol._
//import spray.json._
//
//val graph: RunnableGraph[Future[IOResult]] =
//source.via(flow).to(Sink.foreach(cust => println(cust.toJson)))

