package uk.co.britishgas.streams.oam

import java.nio.file.{Path, Paths}

import java.time.Instant

import akka.{Done, NotUsed}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.scaladsl.{FileIO, Flow, Framing, Keep, Sink, Source}
import akka.stream.{IOResult, ThrottleMode}
import akka.util.ByteString

import spray.json._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success, Try}

import uk.co.britishgas.streams._

/** Batch creator of OAM records.
 * An Akka stream workflow which will process a potentially unbounded source
 * of "customer" elements and create an OAM record for each by consuming an
 * API - specifically making HTTP POST Requests on /users.
 * On completion, this client generates two logs - Success and Failure.
 */
object Client extends App {

  private val log: LoggingAdapter           = Logging.getLogger(system, this)
  private val logRequest: LoggingAdapter    = Logging.getLogger(system, "requests")
  private val logSuccess: LoggingAdapter    = Logging.getLogger(system, "success")
  private val logFailure: LoggingAdapter    = Logging.getLogger(system, "failure")
  private val logException: LoggingAdapter  = Logging.getLogger(system, "exception")
  private val logAnalytics: LoggingAdapter  = Logging.getLogger(system, "analytics")
  private val apiHost: String               = conf("api-host")
  private val apiPath: String               = conf("api-path")
  private val apiPort: Int                  = conf("api-port").toInt
  private val throttleElements: Int         = conf("throttle-elements").toInt
  private val throttlePer: Int              = conf("throttle-per").toInt
  private val throttleBurst: Int            = conf("throttle-burst").toInt
  private val origin: String                = conf("origin")
  private val contentType: String           = conf("content-type")
  private val clientId: String              = conf("cid")
  private val backendUsersKey: String       = conf("buk")
  private val sourceDir: String             = conf("source-dir")
  private val dirPath: String               = if(sourceDir.trim.endsWith("/")) sourceDir else sourceDir + "/"
  private val filePath: String              = dirPath + conf("source-file")
  private val path: Path                    = Paths.get(filePath)
  private val jobId: Int                    = Random.nextInt(100000000)

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

  private def buildJson(in: String): Option[String] = {
    import Marshallers._
    marshalCustomer(in) match {
      case Success(json) => Option(json)
      case Failure(ex) => {
        exceptions += 1
        logException.error(s"JSON Marshalling failed because input[$in] caused: ${ex}")
        Option(null)
      }
    }
  }

  private val source: Source[ByteString, Future[IOResult]] = FileIO.fromPath(path)

  private val delimiter: Flow[ByteString, ByteString, NotUsed] =
    Flow[ByteString].via(Framing.delimiter(ByteString(System.lineSeparator), 10000))

  private val throttle: Flow[ByteString, ByteString, NotUsed] =
    Flow[ByteString].throttle(throttleElements, throttlePer.second, throttleBurst, ThrottleMode.shaping)

  private val marshaller: Flow[ByteString, (String, String), NotUsed] = Flow[ByteString].
    map((bs: ByteString) => buildJson(bs.utf8String)).
    filter((op: Option[String]) => op.nonEmpty).
    map((op: Option[String]) => {
      val json: String = op.get
      val id: String = extractId(json)
      (id, json)
    })

  private val connection: Flow[(HttpRequest, String), (Try[HttpResponse], String), Http.HostConnectionPool] =
    Http().cachedHostConnectionPoolHttps[String](apiHost, apiPort)

  private def extractId(jst: String): String =
    jst.parseJson.asJsObject.fields("data").asJsObject.fields("id").toString

  var elements, successes, failures, exceptions = 0

  val sink: Sink[(Try[HttpResponse], String), Future[Done]] = Sink.foreach[(Try[HttpResponse], String)]({
    case (util.Success(resp), ucrn) =>
      resp.status.intValue match {
        case 201 => resp.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { (body: ByteString) =>
          successes += 1
          logSuccess.info("Created 201 for UCRN: " + ucrn + " Payload " + body.utf8String)

        }
        case status => resp.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { (body: ByteString) =>
          failures += 1
          logFailure.error(status + " for UCRN: " + ucrn + " Payload " + body.utf8String)
        }
      }
    case (util.Failure(ex), ucrn) => {
      exceptions += 1
      logException.error("for UCRN " + ucrn + " Problem: " + ex.getMessage)
    }
  })

  val workflow: Unit =
    source.
      via(delimiter).
      via(throttle).
      map((bs: ByteString) => { elements += 1; bs }). // log element before marshaller in case of marshalling exception
      via(marshaller).
      map((tup: (String, String)) => (buildRequest(tup._2), tup._1)).
      map((tup: (HttpRequest, String)) => {
        tup._1.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { (body: ByteString) =>
          logRequest.info(body.utf8String)
        }
        (tup._1, tup._2)
      }).
      via(connection).
      runWith(sink).onComplete((x: Try[Done]) => {
        analytics()
        system.terminate()
    })

  //entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body => body.utf8String }

  def analytics(): Unit = {
    Thread.sleep(1000) // Block briefly to ensure all analytics are captured before we try and access them.
    logAnalytics.info {
      s"\n\n Terminating job $jobId at ${Instant.ofEpochMilli(System.currentTimeMillis())} \n" +
        s" Writing Analytics ...... \n\n" +
        s" Total elements processed  = $elements \n" +
        s" ---------------------------------------------------------------- \n" +
        s" Successes                 = $successes \n" +
        s" Failures                  = $failures \n" +
        s" Exceptions                = $exceptions \n" +
        s" ---------------------------------------------------------------- \n" +
        s" Total elements reported   = ${successes + failures + exceptions} \n" +
        s" ---------------------------------------------------------------- \n" +
        s" Mismatches ???            = ${elements - (successes + failures + exceptions)} \n "
    }
  }

  logAnalytics.info(
    s"Starting BF job $jobId \n\n" +
      "Data source: " + filePath + "\n" +
      "Consuming endpoint: https://" + apiHost + ":" + apiPort + apiPath +
      " (at rate: " + throttleElements + " request(s) / " + throttlePer + " sec(s)) \n"
  )

}