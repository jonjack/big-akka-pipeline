import java.nio.file.{Path, Paths}

import akka.event.{Logging, LoggingAdapter}
import akka.stream.IOResult
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent.Future

object Test extends App {

//  val log: LoggingAdapter = Logging.getLogger(system, this)
//  log.debug("LOGGED BY LOG")
//  log.info("LOGGED BY LOG")
//
//  printr("API rate: " + conf("api-rate"))
//  printr("Inbound dir: " + conf("inbound-dir"))
//  printr("Source file: " + conf("source"))
//
//  val path: String = conf("inbound-dir") + "/" + conf("source")
//  val file: Path = Paths.get(path)
//
//  val source: Source[ByteString, Future[IOResult]] = FileIO.fromPath(file)
//
//  //val flow: Flow[ByteString, String, NotUsed] = Flow[ByteString].map(i => "Number " + i.toString())
//
//
//  val graph: RunnableGraph[Future[IOResult]] = source.via(Flow[ByteString].map(s => s.utf8String)).to(Sink.foreach(println))
//
//  val sum: Future[IOResult] = graph.run()
//
//  sum.onComplete(xs => system.terminate())

}
