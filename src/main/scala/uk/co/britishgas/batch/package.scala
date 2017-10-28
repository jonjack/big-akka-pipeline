package uk.co.britishgas

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

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

}
