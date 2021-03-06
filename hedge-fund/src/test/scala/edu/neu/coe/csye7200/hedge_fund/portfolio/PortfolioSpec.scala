package edu.neu.coe.csye7200.hedge_fund.portfolio

import org.scalatest.{BeforeAndAfterAll, Inside, Matchers, WordSpecLike}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit._

import scala.concurrent.duration._
import org.scalatest.Inside
import akka.actor.actorRef2Scala
import com.typesafe.config.ConfigFactory
import edu.neu.coe.csye7200.hedge_fund.HedgeFund
import edu.neu.coe.csye7200.hedge_fund.actors._
import edu.neu.coe.csye7200.hedge_fund.model.GoogleOptionModel
import org.scalatest.tagobjects.Slow

/**
 * This specification really tests much of the HedgeFund app but because it particularly deals with
 * processing data from the YQL (Yahoo Query Language) using JSON, we call it by its given name.
 */
class PortfolioSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with Matchers with Inside with BeforeAndAfterAll {

  def this() = this(ActorSystem("MockPortfolioBlackboard"))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
  
  "read portfolio" taggedAs(Slow) in {
    val config = ConfigFactory.load
    val portfolio = HedgeFund.getPortfolio(config)
    portfolio.name shouldEqual "Test Portfolio"
    println(s"portfolio: $portfolio")
  }

  "send back" taggedAs(Slow) in {
    val model = new GoogleOptionModel()
    val blackboard = system.actorOf(Props.create(classOf[MockPortfolioBlackboard], testActor), "blackboard")
    blackboard ! CandidateOption(model, "XX375", true, Map("strike" -> "45.2"), Map("underlying_id" -> "1234"))
    val confirmationMsg = expectMsgClass(3.seconds, classOf[Confirmation])
    println("confirmation msg received: " + confirmationMsg)
    inside(confirmationMsg) {
      case Confirmation(id, model, details) =>
        println(s"confirmation1 details: $details")
        id shouldEqual "XX375"
        blackboard ! KnowledgeUpdate(model, "XX", Map("id" -> "1234"))
        val confirmationMsg2 = expectMsgClass(3.seconds, classOf[Confirmation])
        println("confirmation msg2 received: " + confirmationMsg2)
        // Note that the key "id" is in the model for symbols, not options
        blackboard ! OptionQuery("id", "1234")
        val responseMsg = expectMsgClass(3.seconds, classOf[QueryResponse])
        println("msg received: " + responseMsg)
        inside(responseMsg) {
          case QueryResponse(symbol, attributes) =>
            symbol shouldEqual "XX"
            println(s"attributes: $attributes")
        }
    }
  }
}

class MockPortfolioBlackboard(testActor: ActorRef) extends Blackboard(Map(classOf[KnowledgeUpdate] -> "marketData", classOf[SymbolQuery] -> "marketData", classOf[OptionQuery] -> "marketData", classOf[CandidateOption] -> "optionAnalyzer", classOf[PortfolioUpdate] -> "updateLogger", classOf[Confirmation] -> "updateLogger"),
  Map("marketData" -> classOf[MarketData], "optionAnalyzer" -> classOf[OptionAnalyzer], "updateLogger" -> classOf[UpdateLogger])) {

  override def receive =
    {
      case msg: Confirmation => testActor forward msg
      case msg: QueryResponse => testActor forward msg
//      case msg: CandidateOption => testActor forward msg
      case msg => super.receive(msg)
    }
}

