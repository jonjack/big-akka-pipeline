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

  logAnalytics.info(
    s"Starting BF job $jobId \n\n" +
      "Data source: " + filePath + "\n" +
      "Consuming endpoint: https://" + apiHost + ":" + apiPort + apiPath +
      " (at rate: " + throttleElements + " request(s) / " + throttlePer + " sec(s)) \n"
  )

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

  val logFlow: Flow[(Try[HttpResponse], String), (Try[HttpResponse], String), NotUsed] =
    Flow[(Try[HttpResponse], String)]
      .map(i => {
        i match {
          case (util.Success(resp), ucrn) => logSuccess.info("### RESP" + resp + " UCRN: " + ucrn)
          case (util.Failure(ex), ucrn) => logFailure.info("### EX" + ex + " UCRN: " + ucrn)
        }
          i
      })

//  private val sink = Sink.reduce((Try[HttpResponse], String)) {
//    case (out, (util.Success(resp), ucrn)) => resp
//  }

//  val reduceSink = Sink.reduce[(Try[HttpResponse], String)](_ => {
//    case (map, (util.Success(resp), ucrn)) =>
//  })

  //var elements, successes, failures, exceptions = 0
  var elements = 0    // number of input elements for job (includes any invalid input records)
  var successes = 0   // successful 201 responses
  var failures = 0    // unsuccessful (non-201) responses
  var exceptions = 0  // exceptions - marshalling errors and failed requests

  // Only elements that entered the Connection will reach the Sink
  // ie. elements that failed marshalling will not have got this far.
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
//    case (util.Success(resp), ucrn) if resp.status.intValue() == 201 => successes += 1
//    case (util.Success(resp), ucrn) if resp.status.intValue() != 201 => failures += 1
//    case (util.Failure(ex), ucrn) => exceptions += 1
//    case (util.Success(resp), ucrn) if resp.status.intValue() == 201 => (ucrn, resp.status) :: successes
//    case (util.Success(resp), ucrn) if resp.status.intValue() != 201 => (ucrn, resp.status) :: successes
//    case (util.Failure(ex), ucrn) => (ucrn, ex) :: failures
  })

  val realTimeWorkflow: Unit =
    source.
      via(delimiter).
      via(throttle).
      map((bs: ByteString) => { elements += 1; bs }).
      via(marshaller).
      map((tup: (String, String)) => (buildRequest(tup._2), tup._1)).
      map((tup: (HttpRequest, String)) => {
        logRequest.info(tup._1.toString())
        (tup._1, tup._2)
      }).
      via(connection).
      //via(logFlow).
      runWith(sink).onComplete((x: Try[Done]) => {
        analytics()
        system.terminate()
    })

  def analytics(): Unit = {
    Thread.sleep(1000) // ensure all analytics are captured before we try and access them.
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

//  val workflow: Future[Map[String, HttpResponse]] =
//    source.
//      via(delimiter).
//      via(throttle).
//      via(marshaller).
//      map((tup: (String, String)) => (buildRequest(tup._2), tup._1)).
//      map((tup: (HttpRequest, String)) => {
//        logRequest.info(tup._1.toString())
//        (tup._1, tup._2)
//      }).
//      via(connection).
//      // This folds over the complete Map which means that everything inside it is done in the future,
//      // even the logging. If we want to write results in realtime then we need to use something
//      // like the realTimeWorkflow
//      runFold(Map.empty[String, HttpResponse]) {
//        case (map, (util.Success(resp), ucrn)) => {
//          resp.status.intValue match {
//            case 201 => resp.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
//              logSuccess.info("Created 201 [" + ucrn + "] BODY[" + body.utf8String + "]")
//            }
//            case status => resp.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
//              logFailure.error("Status " + status + " [" + ucrn + "] BODY[" + body.utf8String + "]")
//            }
//          }
//          map ++ Map(ucrn -> resp)}
//        case (map, (util.Failure(ex), symbol)) => {
//          logFailure.error("Exception " + ex.getMessage)
//          map
//        }
//      }
//
//  workflow.onComplete((hr: Try[Map[String, HttpResponse]]) => {
//    val respMap: Map[String, HttpResponse] = hr.get
//    val total: Int = respMap.size
//    val successes: Int = respMap.count(x => x._2.status.intValue() == 201)
//    val failures: Int = respMap.count(x => x._2.status.intValue() != 201)
//    logAnalytics.info {
//      s"Terminating job $jobId \n\n" +
//        s"/------------ Analytics (job $jobId) ------------/\n\n" +
//        " Total elements processed  = " + respMap.size + "\n" +
//        " Successes                 = " + successes + "\n" +
//        " Failures                  = " + failures + "\n" +
//        " Mismatches (Network?)     = " + (total - (successes + failures)) + "  \n"
//    }
//    system.terminate()
//  })

}