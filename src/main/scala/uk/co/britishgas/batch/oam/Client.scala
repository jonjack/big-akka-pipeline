package uk.co.britishgas.batch.oam

import java.nio.file.{Path, Paths}

import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{FileIO, Flow, Framing, Sink, Source}
import akka.stream.{IOResult, ThrottleMode}
import akka.util.ByteString
import uk.co.britishgas.batch._
import uk.co.britishgas.batch.oam.Marshallers._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

/** Batch creator of OAM records.
 * An Akka stream workflow which will process a potentially unbounded source
 * of "customer" elements and create an OAM record for each by consuming an
 * API - specifically making HTTP POST Requests on /users.
 * On completion, this client generates two logs - Success and Failure.
 *
 * A note on throttling
 * --------------------
 * The flow incorporates a throttle to control the rate at which the stream of elements
 * is emitted through the flow, otherwise we will overwhelm the API with too many requests
 * which will likely result in failures.
 * rate = elements / per sec
 * throttle(elements: Int, per: FiniteDuration, maximumBurst: Int, mode: ThrottleMode)
 * burst is set to rate - this could be increased if performance issues are encountered.
 * Throttle mode should be "shaping" since this does not throw exceptions in the event of
 * backpressure.
 */
object Client extends App {

  private val log: LoggingAdapter = Logging.getLogger(system, this)
  private val logsuccess: LoggingAdapter = Logging.getLogger(system, "success")
  private val logfailure: LoggingAdapter = Logging.getLogger(system, "failure")

  private val file: String = conf("source-dir") + "/" + conf("source-file")
  private val path: Path = Paths.get(file)

  //private val protocol: String = conf("protocol")
  private val apiHost: String = conf("api-host")
  private val apiPort: Int = conf("api-port").toInt
  private val apiPath: String = conf("api-path")
 // private val endpoint: String = protocol + "://" + apiHost + apiPath

  private val throttleRate: Int = conf("throttle-rate").toInt
  private val throttleBurst: Int = conf("throttle-burst").toInt

  log.info("Source: " + path)
  //log.info("Consuming endpoint: " + endpoint + " at a rate of " + apiRate + " element(s)/sec.")

  import scala.concurrent.ExecutionContext.Implicits.global

  //val conn = Http().cachedHostConnectionPoolHttps,[Int]("x.x.x.x", 443)

  val source: Source[ByteString, Future[IOResult]] = FileIO.fromPath(path)

  import uk.co.britishgas.batch.oam.Marshallers._

  val throttle = Flow[ByteString].
    via(Framing.delimiter(ByteString(System.lineSeparator), 10000)).
    throttle(throttleRate, 1.second, throttleBurst, ThrottleMode.shaping).
    map((bs: ByteString) => { buildJson(bs.utf8String) }).
    filter((op: Option[String]) => { op.nonEmpty }).
    map((op: Option[String]) => op.get)

  val connection = Http().cachedHostConnectionPoolHttps[String](apiHost, apiPort)

  val sink = Sink.foreach(println)

  val graph: Unit = source.via(throttle).runWith(sink).onComplete(_ => system.terminate())

//  val fut: Future[Map[String, HttpResponse]] =
//    source.
//      via(throttle).
//      map(symbol => (HttpRequest(GET, s"/v1/stats/$symbol"), symbol)). // we are using the coin symbol as the request ID
//      //via(conn).


  /*
  val connection: Flow[(HttpRequest, String), (Try[HttpResponse], String), Http.HostConnectionPool] =
    Http().cachedHostConnectionPoolHttps[String]("api.bitfinex.com", apiPort)

  val flow1 = source.
    via(Framing.delimiter(ByteString(System.lineSeparator), 10000)).
    throttle(apiRate, 1.second, apiRate, ThrottleMode.shaping).
    map((bs: ByteString) => {log.info(bs.utf8String);bs.utf8String})

  val conn = Http().cachedHostConnectionPoolHttps[String]("api.bitfinex.com", 443)
  val symbols = List("btcusd", "ethusd", "omgusd", "xxxusd")  // xxxusd tests the non-200 Response Status case
  val fut: Future[Map[String, HttpResponse]] =
    Source(symbols).
      map(symbol => (HttpRequest(GET, s"/v1/stats/$symbol"), symbol)). // we are using the coin symbol as the request ID
      via(conn).
      runFold(Map.empty[String, HttpResponse]) {

        // This case drills down into successful Responses to log based on whether we got a HTTP Status code 200 or not
        case (map, (util.Success(resp), symbol)) => {
          resp.status.intValue match {
            case 200 => resp.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
              logsuccess.info("Status OK 200 [" + symbol + "] BODY[" + body.utf8String + "]")
            }
            case status => resp.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
              logfailure.info("Status " + status + " [" + symbol + "] BODY[" + body.utf8String + "]")
            }
          }
          map ++ Map(symbol -> resp)}

        // This case catches the situations where we did not get a response back for some reason.
        case (map, (util.Failure(ex), symbol)) => {
          logfailure.info("Exception " + ex.getMessage)
          map
        }
      }
      */
  /*
   * How to find out if the response can be marshalled into a { data: ... } or { errors: ... }
   * Json Api Response?
   * See - https://github.com/spray/spray-json#providing-jsonformats-for-unboxed-types
   * We could try and use regular expressions and match on those for the response body.
   *
   */
  /*
  val flow = Flow[ByteString].
    via(Framing.delimiter(ByteString(System.lineSeparator), 10000)).
    throttle(apiRate, 1.second, apiRate, ThrottleMode.shaping).
    map((bs: ByteString) => {log.info(bs.utf8String);bs.utf8String})

  */

  /*
   * The secret to getting this to work was to use runWith(sink) rather than to(sink).
   */
  //val graph: Unit = source.via(flow).runWith(sink).onComplete(_ => system.terminate())
  //val graph: Unit = source.via(flow).runWith(sink).onComplete(_ => system.terminate())



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

