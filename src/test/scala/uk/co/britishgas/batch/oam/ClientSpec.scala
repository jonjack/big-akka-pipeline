package uk.co.britishgas.batch.oam

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{FileIO, Flow, Framing, Keep, Sink, Source}
import akka.stream.{IOResult, ThrottleMode}
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import akka.util.ByteString
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers, WordSpecLike}
import uk.co.britishgas.batch._
import uk.co.britishgas.batch.oam._
import uk.co.britishgas.batch.oam.Client._

import scala.collection.immutable
import scala.concurrent.{Await, Future}

class ClientSpec extends TestKit(ActorSystem("ClientSpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {


  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "An Echo actor" must {

    "send back messages unchanged" in {
      val echo: ActorRef = system.actorOf(TestActors.echoActorProps)
      echo ! "check"
      expectMsg("check")
    }

  }

  //val sourceUnderTest = Source.repeat(1).map(_ * 2)

//  private val config: Config = ConfigFactory.load()
//
//  val sourceDir = config.getString("source-dir")
//
//  //val sourceDir = ConfigFactory.parseString("source-dir")
//
//  println("Sourcedir: " + sourceDir + " and Path: " + Client.file)
//
//  val sink = Sink.ignore
//
//  val future = source
//                .take(1)
//                .map(x => println(x.utf8String))
//    .toMat(sink)(Keep.right)

//  val result = Await.result(future, 3.seconds)
//  assert(result == Seq.fill(10)(2))
}
