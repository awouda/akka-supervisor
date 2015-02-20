package com.jtmconsultancy.actor

import akka.actor.SupervisorStrategy.{Restart, Stop, Decider}
import akka.actor._
import akka.actor.Actor.Receive

import scala.util.Random

case object Start

case class Transaction(iban:Int)

class MasterActor() extends Actor {
  var caller:ActorRef = _

  val random = new Random()
  override def receive: Receive = {
    case Start =>
      caller = sender
      context.actorOf(Props(new TransactionAggregatorActor(random.nextInt(100)+999))) ! Start
    case s:String   => caller ! s
  }


}



class TransactionAggregatorActor(iban:Int ) extends Actor {

  val worker = context.actorOf(Props(new WorkerActor))

  var caller:ActorRef = _

  override def receive: Receive = {
    case Start =>
      caller = sender()

      worker ! Transaction(iban)
    case s:String   => caller ! s
  }

  final val defaultStrategy: SupervisorStrategy = {
    def defaultDecider: Decider = {
      case _: IllegalArgumentException ⇒ Restart
      case _: Exception                ⇒ Stop
    }
    OneForOneStrategy()(defaultDecider)
  }
}



class WorkerActor() extends Actor {

  println("Started")


  val random = new Random()

  override def receive = {
    case msg:Transaction =>



      if (random.nextBoolean()) {

        throw new IllegalArgumentException("Crash!!!")
      } else {
        sender ! s"did some work for  ${msg.iban} "
      }
  }

  override def preRestart(
    reason: Throwable, message: Option[Any]) {
    // retry
    message foreach { self forward _ }
  }
}
