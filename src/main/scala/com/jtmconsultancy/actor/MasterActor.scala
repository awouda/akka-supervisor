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
 class ServiceException(msg:String) extends Exception(msg)

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

  context.setReceiveTimeout(130 milliseconds)


  override def receive: Receive = {

    case Start =>
      caller = sender()

      worker ! Transaction(iban)
    case s:String   =>

      caller ! s
    case ReceiveTimeout =>  worker ! Transaction(iban)


  }


  final val defaultStrategy: SupervisorStrategy = {
    def defaultDecider: Decider = {
      case _: ServiceException ⇒ Restart
      case _: Exception        ⇒ Stop
    }
    OneForOneStrategy()(defaultDecider)
  }

}



class WorkerActor() extends Actor {

  import context.dispatcher

  val random = new Random()

  override def receive = {
    case msg:Transaction =>

      val waited = random.nextInt(100)+100
      
      if (waited %2 ==0) throw new ServiceException("Oops random crash!!")

      context.system.scheduler.scheduleOnce(waited milliseconds,sender, s"did some work for  ${msg.iban}" )
      println(s"waited ${waited} before sending back msg")

  }

  override def preRestart(reason: Throwable, message: Option[Any]) {
    println("trying again after crash")
    message foreach { self forward _ }
  }


}


