package com.jtmconsultancy.actor

import akka.actor.SupervisorStrategy.{Restart, Stop, Decider}
import akka.actor._
import akka.actor.Actor.Receive
import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.duration._

import scala.util.Random

case object Start

case class Transaction(iban:Int)
 class ServiceTimeoutException(msg:String) extends Exception(msg)

class MasterActor() extends Actor {
  var caller:ActorRef = _

  val random = new Random()
  override def receive: Receive = {
    case Start =>
      caller = sender
      context.actorOf(Props(new TransactionAggregatorActor(random.nextInt(100)+999)), "aggregationactor") ! Start
    case s:String   => caller ! s
  }
}



class TransactionAggregatorActor(iban:Int ) extends Actor {

  val worker = context.actorOf(Props(new WorkerActor), "workeractor")

  var caller:ActorRef = _


  override def receive: Receive = {
    case Start =>
      caller = sender()

      worker ! Transaction(iban)
    case s:String   => caller ! s

  }

}



class WorkerActor() extends Actor {

  import akka.pattern.{ask,pipe}
  import context.dispatcher

  implicit val timeout = Timeout(140 milliseconds)


  val random = new Random()

   var caller:ActorRef = _

  override def receive = {
    case msg:Transaction =>
      caller = sender()

      val result = (context.actorOf(Props(new ServiceActor),"serviceactor") ? msg).mapTo[String]
      result pipeTo sender
    case s:String =>
      caller ! s
  }




  final val defaultStrategy: SupervisorStrategy = {
    def defaultDecider: Decider = {
      case _: ServiceTimeoutException ⇒ Restart
      case _: Exception                ⇒ Stop
    }
    OneForOneStrategy()(defaultDecider)
  }
}




class ServiceActor() extends Actor {

  val random = new Random()

  val start = System.currentTimeMillis()

  override def receive = {
    case msg:Transaction =>

      val waited = random.nextInt(100)+100

      if (waited > 140) {
        throw new ServiceTimeoutException("Service timed out!!")
      }

    sender ! s"did some work for  ${msg.iban} "

  }

  override def preRestart(
    reason: Throwable, message: Option[Any]) {
    // retry
    println("retrying message")
    message foreach { self forward _ }
  }
}




