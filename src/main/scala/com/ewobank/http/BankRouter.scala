package com.ewobank.http
import akka.http.scaladsl.server.Directives._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.headers.Location
import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import com.ewobank.actors.PersistantUserAccount.{Command, Response}
import com.ewobank.actors.PersistantUserAccount.Command._
import com.ewobank.actors.PersistantUserAccount.Response._
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._


case class BankAccountCreationRequest(user:String,currency: String,balance: Double){
  def toCommand(replyTo: ActorRef[Response]):Command = CreateAccount(user,currency,balance,replyTo)
}
case class BankAccountUpdatedRequest(currency: String,ammount: Double){
  def toCommand(id:String,replyTo:ActorRef[Response]): Command = UpdateAccount(id,currency,ammount,replyTo)
}
case class FailureResponse(reason: String)
case class SuccessResponse(response: String)

class BankRouter(bank:ActorRef[Command])(implicit system: ActorSystem[_]) {

  implicit  val timeout: Timeout = Timeout(5.seconds)

  def createBankAccount(request: BankAccountCreationRequest): Future[Response] =
    bank.ask(replyTo => request.toCommand(replyTo))

  def updateBankAccount(id: String,request: BankAccountUpdatedRequest):Future[Response]=
    bank.ask(replyTo => request.toCommand(id,replyTo))

  def getBankAccount(id:String): Future[Response]=
    bank.ask(replyTo => GetAccount(id ,replyTo))

/*  post /bank
      payload: bank account creation request as JSON
      response: 201 creates
      lactioin : bank/uuid

    get /bank/uuid
      response: 200 json rep of bank acc details - 404 not found

    put /bank/uuid
      PAYLOAD (currency, amount) as Json
      Response:
        1. 200ok, payload: new bank details
        2. 404 not found
        3. TODO 400 bad request
 */
  val  routes =
    pathPrefix("bank"){
      pathEndOrSingleSlash{
        post{
          //parse the payload
          entity(as[BankAccountCreationRequest]){ request=>
            /*
            -convert the request to a command
            -send the command to the bank
            - expect a reply
             */
            onSuccess(createBankAccount(request)){
              // - send back an hhp response
              case BankAccountCreatedResponse(id) =>
                respondWithHeader(Location(s"/bank/$id")){
                  complete(201,SuccessResponse(s"$id"))
                }
            }
          }
        }
      }~
      path(Segment){ id =>
        get{
          /*
          send a command to bank
          espect response
          send back responses
           */
          onSuccess(getBankAccount(id)) {
            case GetBankAccountResponse(Some(account)) =>
              complete(account)
            case GetBankAccountResponse(None) => complete(404, FailureResponse(s"Bank account $id cannot be found"))
          }
        }~
          put{
            entity(as[BankAccountUpdatedRequest]){ request =>
              onSuccess(updateBankAccount(id, request)){
                case BankAccountBalanceUpdatedResponse(Some(account))=>
                  complete(account)
                case BankAccountBalanceUpdatedResponse(None)=>
                  complete(404, FailureResponse(s"Bank account $id cannot be found"))
              }
            }
            /*
            -transform the request to a command
            - send the command to the bank
            -expect a reply
            -return a response
             */
          }


      }
    }

}
