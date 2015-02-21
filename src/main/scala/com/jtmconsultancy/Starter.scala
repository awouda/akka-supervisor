package com.jtmconsultancy

import akka.actor.{Props, ActorSystem}
import akka.util.Timeout
import com.jtmconsultancy.actor.{Start, MasterActor}
import akka.pattern.ask
import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by alex on 19-2-15.
 */
object Starter extends App {

  implicit  val timeout = Timeout (900 milliseconds)

  val system = ActorSystem("akkaPerf")

  val master = system.actorOf(Props(new MasterActor()))

  val result =  master ? Start

  println(Await.result(result,Duration.Inf)) // blocks

  system.shutdown()
}
