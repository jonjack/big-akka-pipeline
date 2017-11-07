package uk.co.britishgas.batch.oam

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import akka.testkit.TestKit
import akka.util.ByteString
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import uk.co.britishgas.batch._

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Try

class ClientSpec extends TestKit(ActorSystem("ClientSpec")) with FlatSpecLike with Matchers
  with Client  with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val testSource: String =  "003005400001|Ms|Tera|Patrick|terapatrick@mail.com|BG,SE|active|CRM\n" +
                    "003005400002|Mr|Harry|Callahan|harrycallahan@mail.com|SE|active|AWB\n" +
                    "003005400003|Mrs|Cindy|Smith|cindysmith@hotmail.com|BG,SE|active|PPOT3\n"

  val testRecord: String =  "003005400001|Ms|Tera|Patrick|terapatrick@mail.com|BG,SE|active|CRM"

  val future: Future[immutable.Seq[ByteString]] = source.runWith(Sink.seq)
  val result: immutable.Seq[ByteString] = Await.result(future, 3.seconds)
  val asutf8: String = {
      Try { result.fold(ByteString(""))((_: ByteString) ++ (_: ByteString)).decodeString("UTF8") }
    }.get

  "Reading from an OAM file source" should "return a non-empty Vector of ByteStrings" in {
    assert(result.isInstanceOf[Vector[ByteString]])
    assert(result.nonEmpty)
  }

  it should "represent an expected utf-8 string" in {
    //val asutf8: String = result.fold(ByteString(""))(_ ++ _).decodeString("UTF8")
    assert(asutf8 == testSource)
  }

  it should "be delimited by new lines" in {
    val future: Future[immutable.Seq[ByteString]] = source.via(delimiter).take(1).runWith(Sink.seq)
    val result: immutable.Seq[ByteString] = Await.result(future, 3.seconds)
    val asutf8: String = {
      Try { result.fold(ByteString(""))((_: ByteString) ++ (_: ByteString)).decodeString("UTF8") }
    }.get
    assert(asutf8 == testRecord)
  }

}
