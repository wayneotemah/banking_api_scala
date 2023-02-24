package com.ewobank.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.Persistence
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
//import akka.remote.transport.TestTransport.Behavior

import java.util.Currency

// single user account
class PersistantUserAccount {
//  commands => messages
  sealed  trait Command
  case class CreateAccount(user:String,currency:String,initialBalance:Double,replyTo:ActorRef[Response]) extends Command
  case class UpdateAccount(id:String,currency:String,amount:Double,replyTo:ActorRef[Response]) extends Command
  case class GetAccount(id:String,replayTo:ActorRef[Response]) extends Command


  //  events => to persist to casandra
  trait Event
  case class  AccountCreated(bankAccount: BankAccount) extends Event
  case class BalanceUpdated(amount:Double) extends Event


  //  state
  case class BankAccount(id:String,user:String,currency: String,balance:Double)

//  response

  sealed trait Response
  case class BankAccountCreatedResponse(id:String) extends Response
  case class BankAccountBalanceUpdatedResponse(mayBeBankAccount:Option[BankAccount]) extends Response
  case class GetBankAccountResponse(mayBeBankAccount:Option[BankAccount]) extends Response

  //Account Persistent actor
  // command handler => message handler => persisted an event
  // event handler => update state
  // state

    val commandHandler: (BankAccount,Command) => Effect[Event,BankAccount] = (state,command)=>
    command match {
      case CreateAccount(user, currency, initialBalance, bank) =>
        val id = state.id
        Effect.persist(AccountCreated(BankAccount(id, user, currency, initialBalance)))
              .thenReply(bank)(_ => BankAccountCreatedResponse(id))

      case UpdateAccount(_,_,amount,replyTo)=>
        val newBalance = state.balance + amount
        if (newBalance< 0)
          Effect.reply(replyTo)(BankAccountBalanceUpdatedResponse(None))
        else
          Effect.persist(BalanceUpdated(amount)).thenReply(replyTo)(newBalance=> BankAccountBalanceUpdatedResponse(Some(newBalance)))

      case GetAccount(_,replayTo) =>
        Effect.reply(replayTo)(GetBankAccountResponse(Some(state)))

    }

  val eventHandler: (BankAccount,Event) => BankAccount = (state,event)=>
     event match {
    case AccountCreated(bankAccount) =>
      bankAccount
    case BalanceUpdated(amount) =>
      state.copy(balance = state.balance + amount)
   }


  def apply(id: String): Behavior[Command] =
    EventSourcedBehavior[Command,Event,BankAccount](
      persistenceId = PersistenceId.ofUniqueId(id),
      emptyState = BankAccount(id,"","",0.0),
      commandHandler = commandHandler,
      eventHandler = eventHandler
      )
}
