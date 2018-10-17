package edu.neu.coe.csye7200.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit._
import edu.neu.coe.csye7200.model.Model
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.io.Source
import scala.concurrent.duration._
import spray.http._
import spray.http.MediaTypes._
import org.scalatest.Inside
import org.scalatest.tagobjects.Slow

import scala.language.postfixOps
import spray.json.pimpString

/**
 * This specification tests much of the HedgeFund app but because it particularly deals with
 * processing data from the YQL (Yahoo Query Language) using JSON, we call it by its given name.
 */
class JsonGoogleOptionParserSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("JsonGoogleParserSpec"))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  import scala.language.postfixOps
  val json: String = Source.fromFile(getClass.getResource("/googleOptionExample.json").getPath) mkString

  "json read" in {
    import spray.json._
    val obj = JsonGoogleOptionParser.fix(json).parseJson
  }

  "json conversion" in {
    val contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`)
    val entity = HttpEntity(contentType, json.getBytes())
    val ok = JsonGoogleOptionParser.decode(entity) match {
      case Right(x) =>
        x.puts.length should equal(20)
        val puts = x.puts
        puts.head.get("s") should matchPattern { case Some("MSFT150731P00042500") => }

      case Left(x) =>
        fail("decoding error: " + x)
    }
  }

  "send back" taggedAs Slow in {
    val blackboard = system.actorOf(Props.create(classOf[MockGoogleOptionBlackboard], testActor), "blackboard")
    val entityParser = _system.actorOf(Props.create(classOf[EntityParser], blackboard))
    val contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`)
    val entity = HttpEntity(contentType, json.getBytes()) 
    entityParser ! EntityMessage("json:GO", entity)
    val msg = expectMsgClass(5.seconds, classOf[QueryResponse])
    println("msg received: " + msg)
    msg should matchPattern {
      case QueryResponse(_, _) =>
    }
  }
}

import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await


class MockGoogleOptionUpdateLogger(blackboard: ActorRef) extends UpdateLogger(blackboard) {
  override def processOption(identifier: String, model: Model, attributes: Map[String, Any]): Unit = {
    val keys = model mapKeys List("underlying", "strikePrice", "expiry")
    println(s"$keys")
    val future = blackboard ? OptionQuery(identifier, keys)
    val result = Await.result(future, timeout.duration).asInstanceOf[QueryResponse]
    blackboard ! result
  }
}

class MockGoogleOptionBlackboard(testActor: ActorRef) extends Blackboard(Map(classOf[KnowledgeUpdate] -> "marketData", classOf[SymbolQuery] -> "marketData", classOf[OptionQuery] -> "marketData", classOf[CandidateOption] -> "optionAnalyzer", classOf[Confirmation] -> "updateLogger"),
  Map("marketData" -> classOf[MarketData], "optionAnalyzer" -> classOf[OptionAnalyzer], "updateLogger" -> classOf[MockGoogleOptionUpdateLogger])) {

  override def receive: PartialFunction[Any, Unit] =
    {
      case msg: Confirmation => msg match {
        // Cut down on the volume of messages
        case Confirmation("MSFT150731P00045000", _, _) => super.receive(msg)
        case _ =>
      }
      case msg: QueryResponse => testActor forward msg
      case msg => super.receive(msg)
    }
}
