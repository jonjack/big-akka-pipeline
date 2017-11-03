package uk.co.britishgas.batch.oam

import java.nio.file.{Path, Paths}

import akka.NotUsed
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{FileIO, Flow, Framing, Sink, Source}
import akka.stream.{IOResult, ThrottleMode}
import akka.util.ByteString
import spray.json._
import uk.co.britishgas.batch._
import uk.co.britishgas.batch.oam.Marshallers._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Random, Try}

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

  private val log: LoggingAdapter         = Logging.getLogger(system, this)
  private val logrequest: LoggingAdapter  = Logging.getLogger(system, "requests")
  private val logsuccess: LoggingAdapter  = Logging.getLogger(system, "success")
  private val logfailure: LoggingAdapter  = Logging.getLogger(system, "failure")
  private val loganalytics: LoggingAdapter  = Logging.getLogger(system, "analytics")

  private val apiHost: String             = conf("api-host")
  private val apiPath: String             = conf("api-path")
  private val apiPort: Int                = conf("api-port").toInt
  private val throttleRate: Int           = conf("throttle-rate").toInt
  private val throttleBurst: Int          = conf("throttle-burst").toInt
  private val origin: String              = conf("origin")
  private val contentType: String         = conf("content-type")
  private val clientId: String            = conf("cid")
  private val backendUsersKey: String     = conf("buk")
  private val file: String                = conf("source-dir") + "/" + conf("source-file")
  private val path: Path                  = Paths.get(file)

  private val jobId: Int = Random.nextInt(100000000)

  loganalytics.info(
    s"Starting BF job $jobId \n\n" +
      "Data source: " + file + "\n" +
      "Consuming endpoint: https://" + apiHost + ":" + apiPort + apiPath +
      " (at rate: " + throttleRate + " request(s)/sec) \n"
  )

  private val source: Source[ByteString, Future[IOResult]] = FileIO.fromPath(path)

  /** A Flow of type ByteString => ByteString which splits a stream of ByteStrings into lines.
    */
  private val delimiter: Flow[ByteString, ByteString, NotUsed] =
    Flow[ByteString].via(Framing.delimiter(ByteString(System.lineSeparator), 10000))

  /** A Flow which explicitly creates back pressure on it's upstream source of elements and
    * controls the rate at which elements are emitted downstream in the materialized graph.
    */
  private val throttle = Flow[ByteString].throttle(throttleRate, 1.second, throttleBurst, ThrottleMode.shaping)

  /** A Flow of type ByteString => (String, String) which marshalls a stream of ByteStrings into a
    * JSON representation. The emitted tuple has a logical type of (Customer ID, Customer JSON).
    * This flow is designed to emit into a pooled connection which, because responses are not
    * necessarily in order of request, require some ID to map the Request->Response. In this case,
    * we use the Customer ID as the mapping in order that any logged failures can be traced back
    * to the customer record.
    */
  private val marshaller: Flow[ByteString, (String, String), NotUsed] = Flow[ByteString].
    map((bs: ByteString) => { buildJson(bs.utf8String) }).
    filter((op: Option[String]) => { op.nonEmpty }).
    map((op: Option[String]) => {
      val json: String = op.get
      val id: String = extractId(json)
      (id, json) // emit the Tuple2[String, String] downstream
    })

  private val connection: Flow[(HttpRequest, String), (Try[HttpResponse], String), Http.HostConnectionPool] =
    Http().cachedHostConnectionPoolHttps[String](apiHost, apiPort)

  private def extractId(jst: String) = jst.parseJson.asJsObject.fields("data").asJsObject.fields("id").toString

  import akka.http.scaladsl.model.HttpMethods._
  import akka.http.scaladsl.model.HttpProtocols._
  import akka.http.scaladsl.model.MediaTypes._
  import akka.http.scaladsl.model.{HttpRequest, _}

  private val org = headers.Origin(origin)
  private val cid = RawHeader("X-Client-ID", clientId)
  private val buk = RawHeader("X-Backend-Users-Key", backendUsersKey)
  private val hds = List(org, cid, buk)

  private def buildRequest(body: String): HttpRequest = {
    val entity = HttpEntity(`application/vnd.api+json`, body)
    HttpRequest(POST, apiPath, hds, entity, `HTTP/1.1`)
  }

  val fut: Future[Map[String, HttpResponse]] =
    source.
      via(delimiter).
      via(throttle).
      via(marshaller).
      map((tup: (String, String)) => (buildRequest(tup._2), tup._1)).
      map((tup: (HttpRequest, String)) => {
        logrequest.info(tup._1.toString())
        (tup._1, tup._2)
      }).
      via(connection).
      runFold(Map.empty[String, HttpResponse]) {
        case (map, (util.Success(resp), ucrn)) => {
          resp.status.intValue match {
            case 201 => resp.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
              logsuccess.info("Created 201 [" + ucrn + "] BODY[" + body.utf8String + "]")
            }
            case status => resp.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
              logfailure.info("Status " + status + " [" + ucrn + "] BODY[" + body.utf8String + "]")
            }
          }
          map ++ Map(ucrn -> resp)}
        case (map, (util.Failure(ex), symbol)) => {
          logfailure.info("Exception " + ex.getMessage)
          map
        }
      }

  // Map in the future then terminate the Actor system
  fut.onComplete((hr: Try[Map[String, HttpResponse]]) => {
    val respMap: Map[String, HttpResponse] = hr.get
    val total: Int = respMap.size
    val successes: Int = respMap.filter(x => x._2.status.intValue() == 201).size
    val failures: Int = respMap.filter(x => x._2.status.intValue() != 201).size
    loganalytics.info {
      s"Terminating job $jobId \n\n" +
        s"/------------ Analytics (job $jobId) ------------/\n\n" +
        " Total elements processed  = " + respMap.size + "\n" +
        " Successes                 = " + successes + "\n" +
        " Failures                  = " + failures + "\n" +
        " Mismatches (Network?)     = " + (total - (successes + failures)) + "  \n"
    }
    system.terminate()
  })

}