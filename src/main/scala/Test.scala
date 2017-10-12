import java.nio.file.{Path, Paths}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.scaladsl._
import akka.util.ByteString
import cargo._

import scala.concurrent.Future

object Test extends App {

  printr("API rate: " + conf("api-rate"))
  printr("Inbound dir: " + conf("inbound-dir"))
  printr("Batch file: " + conf("batch-file"))

  val path: String = conf("inbound-dir") + "/" + conf("batch-file")
  val file: Path = Paths.get(path)

  val source: Source[ByteString, Future[IOResult]] = FileIO.fromPath(file)

  //val flow: Flow[ByteString, String, NotUsed] = Flow[ByteString].map(i => "Number " + i.toString())


  val graph: RunnableGraph[Future[IOResult]] = source.via(Flow[ByteString].map(s => s.utf8String)).to(Sink.foreach(println))

  val sum: Future[IOResult] = graph.run()

  sum.onComplete(xs => system.terminate())

}
