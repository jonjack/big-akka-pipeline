import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent._
import scala.util.{Failure, Success}

package object cargo {

  // Get an Actor Factory and Materializer (these will be implicitly used)
  implicit val system = ActorSystem("SimpleAggregator")
  implicit val materializer = ActorMaterializer()

  // We need this to help close down the Actor system onComplete
  implicit val ec = system.dispatcher

  private val config = ConfigFactory.load()
  private val tZero = System.currentTimeMillis()

  def conf(s: String) = config.getString(s)

  def printg(s: Any): Unit = println(Console.GREEN + s + Console.RESET)
  def printr(s: Any): Unit = println(Console.RED + s + Console.RESET)

  def timedFuture[T](name: String)(f: Future[T])(implicit ec: ExecutionContext): Future[T] = {
    val start = System.currentTimeMillis()
    printg(s"--> started $name at t0 + ${start - tZero}")
    f.andThen{
      case Success(t) =>
        val end = System.currentTimeMillis()
        printg(s"\t<-- finished $name after ${end - start} millis")
      case Failure(ex) =>
        val end = System.currentTimeMillis()
        printg(s"\t<X> failed $name, total time elapsed: ${end - start}\n$ex")
    }
  }

  def clearOutputDir() =
    for {
      files <- Option(new File("res").listFiles)
      file <- files if file.getName.endsWith(".csv")
    } file.delete()

}