import java.nio.file.{Path, Paths}

import akka.NotUsed
import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Flow, RunnableGraph, Sink, Source}
import akka.util.ByteString
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future

case class Customer (
                     bpid: String,
                     title: String,
                     firstname: String,
                     surname: String,
                     brand: String,
                     email: String,
                     channel: String)


object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val colorFormat = jsonFormat7(Customer)
}

object FileToCaseClass extends App {

  import cargo._



  val path: String = conf("inbound-dir") + "/" + conf("batch-file")
  val file: Path = Paths.get(path)

  val source: Source[ByteString, Future[IOResult]] = FileIO.fromPath(file)

  val flow: Flow[ByteString, Customer, NotUsed] =
    Flow[ByteString]
      //.via(Framing.delimiter(ByteString("\n"), 1024))
      //.via(Framing.delimiter(ByteString("|"), 1024))
      .map(bs => bs.utf8String.trim.split("|"))
      .map(res =>
          res match {
            case Array(bpid, title, firstname, surname, brand, email, channel) => Customer(bpid, title, firstname, surname, brand, email, channel)
            case _ => Customer("30065400124", "Ms", "Tera", "Patrick", "SE", "tera.patrick@hotmail.com", "POT3")
          })

  import MyJsonProtocol._
  import spray.json._

  val graph: RunnableGraph[Future[IOResult]] =
    source.via(flow).to(Sink.foreach(cust => println(cust.toJson)))


  //val graph: RunnableGraph[Future[IOResult]] =
    //source.via(Flow[ByteString].map(s => s.utf8String)).to(Sink.foreach(println))

  val sum: Future[IOResult] = graph.run()

  sum.onComplete(xs => system.terminate())

}
