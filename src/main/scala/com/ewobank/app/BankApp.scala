package com.ewobank.app

import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.util.Timeout
import com.ewobank.actors.Bank
import com.ewobank.actors.PersistantUserAccount.Command
import com.ewobank.http.BankRouter

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Try,Success,Failure}

object BankApp {

  def startHttpServer(bank: ActorRef[Command])(implicit system: ActorSystem[_]): Unit = {
    implicit val ec:ExecutionContext = system.executionContext
    val router = new BankRouter(bank)
    val routes = router.routes

    val httpBindinfFuture = Http().newServerAt("localhost",8080).bind(routes)
    httpBindinfFuture.onComplete{
      case Success(binding)=>
        val address = binding.localAddress
        system.log.info(s"Server online as http://${address.getHostString}: ${address.getPort}")
      case Failure(ex)=>
        system.log.error(s"failed to start server: $ex")
    }

  }

  def main(args:Array[String]): Unit ={
    trait RootCommand
    case class RetrieveBankActor(replyTo:ActorRef[ActorRef[Command]]) extends RootCommand

    val rootBevahior: Behavior[RootCommand] = Behaviors.setup{ context =>
      val bankActor = context.spawn(Bank(),"bank")
      Behaviors.receiveMessage{
        case RetrieveBankActor(replyTo)=>
          replyTo ! bankActor
          Behaviors.same
      }
    }

    implicit val system:ActorSystem[RootCommand] = ActorSystem(rootBevahior,"Banksystem")
    implicit val timeout:Timeout = Timeout(5.seconds)
    implicit val ec:ExecutionContext = system.executionContext

    val bankActorFuture: Future[ActorRef[Command]] = system.ask(replyTo=> RetrieveBankActor(replyTo))
    bankActorFuture.foreach(startHttpServer)
  }

}
