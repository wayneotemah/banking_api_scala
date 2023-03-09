package com.ewobank.actors

import akka.NotUsed
import akka.actor.typed.delivery.DurableProducerQueue.State
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import java.util.UUID

object Bank {
//  Command = message

  import PersistantUserAccount.Command._
  import PersistantUserAccount.Response._
  import PersistantUserAccount.Command


//  events

//  events
  sealed trait Event
  case class BankAccountCreated(id:String) extends Event

  //  state
  case class State(accounts:Map[String,ActorRef[Command]])


  //  commandHandler
  def commandHandler(context:ActorContext[Command]):(State, Command)=> Effect[Event,State] = (state,command) =>
    command match {
      case createCommand @ CreateAccount(_, _,_, _) =>
        val id = UUID.randomUUID.toString
        val newBankAccount = context.spawn(PersistantUserAccount(id),id)
        Effect.persist(BankAccountCreated(id)).thenReply(newBankAccount)(_=> createCommand )

      case updateCmd @ UpdateAccount(id, _, _, replyTo) =>
        state.accounts.get(id) match {
          case Some(account) =>
            Effect.reply(account)(updateCmd)
          case None =>
            Effect.reply(replyTo)(BankAccountBalanceUpdatedResponse(None))// Failed search
        }

        case getCmd @ GetAccount(id,replyTo) =>state.accounts.get(id)match {
            case Some(account) => Effect.reply(account)(getCmd)
            case None => Effect.reply(replyTo)(GetBankAccountResponse(None)) // Failed search
        }
    }

  // eventHandler
  def eventHandler(context: ActorContext[Command]): (State, Event) => State = (state, event) =>
    event match {
      case BankAccountCreated(id) =>
        val account = context.child(id) // exists after command handler,
          .getOrElse(context.spawn(PersistantUserAccount(id), id)) // does NOT exist in the recovery mode, so needs to be created
          .asInstanceOf[ActorRef[Command]] // harmless, it already has the right type
        state.copy(state.accounts + (id -> account))
    }

  //behavior
  def apply(): Behavior[Command] = Behaviors.setup{ context =>
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId("bank"),
      emptyState = State(Map()),
      commandHandler = commandHandler(context),
      eventHandler = eventHandler(context),
    )

  }
}

object BankPlayground {

  import PersistantUserAccount.Command._
  import PersistantUserAccount.Response._
  import PersistantUserAccount.Response

  def main(args: Array[String]): Unit = {
    val rootBehavior: Behavior[NotUsed] = Behaviors.setup { context =>
      val bank = context.spawn(Bank(), "bank")
      val logger = context.log

      val responseHandler = context.spawn(Behaviors.receiveMessage[Response] {
        case BankAccountCreatedResponse(id) =>
          logger.info(s"successfully created bank account $id")
          Behaviors.same
        case GetBankAccountResponse(maybeBankAccount) =>
          logger.info(s"Account details: $maybeBankAccount")
          Behaviors.same
      }, "replyHandler")

      // ask pattern
      import akka.actor.typed.scaladsl.AskPattern._
      import scala.concurrent.duration._
      implicit val timeout: Timeout = Timeout(2.seconds)
      implicit val scheduler: Scheduler = context.system.scheduler
      implicit val ec: ExecutionContext = context.executionContext

      // test 1
//      bank ! CreateAccount("daniel", "USD", 10, responseHandler)

      // test 2
//      bank ! GetAccount("cd7b81e6-bbf5-4c61-a0f4-7d643c432b8b", responseHandler)

      Behaviors.empty
    }

    val system = ActorSystem(rootBehavior, "BankDemo")
  }
}


