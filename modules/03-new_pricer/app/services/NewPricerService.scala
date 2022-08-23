package newpricer.services

import domain.PricerResponse.Offer
import domain._

import helpers.sorus.Fail
import helpers.sorus.SorusDSL.Sorus
import javax.inject.{ Inject, Singleton }
import newpricer.models.WakamResponse.{ FailureCase, SuccessCase }
import newpricer.models.{ NewPricerConfig, WakamQuote, WakamSubscribe }
import play.api.libs.json.{ JsError, JsSuccess, Json }
import play.api.libs.ws.WSClient
import play.api.{ Configuration, Logging }
import scalaz.{ -\/, \/, \/- }
import utils.NumberUtils.amountFromDoubleToCentime

import scala.concurrent.{ ExecutionContext, Future }

@Singleton
private[newpricer] class NewPricerService @Inject() (
  val ws:          WSClient,
  val config:      Configuration
)(implicit val ec: ExecutionContext)
  extends JsonParser
    with Sorus
    with Logging {

  private[this] val wakam_url = config.get[String]("wakam.url")

  /**
   * @param request : input for the webservice
   * @param config : broker authentication
   * @return
   */
  private[newpricer] def quote(
    request: WakamQuote,
    config:  NewPricerConfig
  ): Future[Fail \/ PricerResponse] = {
    for {
      response <- ws.url(s"$wakam_url/getPrice").addHttpHeaders("Authorization" -> config.key).post(
                    Json.toJson(request)(WakamQuote.wakam_quote_write)
                  )
    } yield {
      response.status match {
        case 200 => response.json.validate[SuccessCase] match {
            case JsSuccess(value, _) => \/-(Offer(
                Price(Amount(amountFromDoubleToCentime(value.MontantTotalPrimeTTC / 12))),
                external_data = Some(Json.obj("quote_reference" -> value.QuoteReference))
              ))
            case JsError(errors)     => -\/(Fail(
                s"Wakam returned status 200 but It is impossible to fetch price from response: $response \n Because of the error: ${errors.map(_._2).mkString(" ")}"
              ))
          }
        case _   => response.json.validate[FailureCase] match {
            case JsSuccess(value, _) => -\/(Fail(value.message))
            case JsError(errors)     => -\/(Fail(
                s"Wakam returned a response with status: ${response.status} along with some unknown error: ${errors.map(_._2).mkString(" ")}"
              ))
          }
      }
    }
  }

  /**
   * @param request : same structure as request: NewPricerQuoteRequest but with more data
   * @param config : broker authentication
   * @param selected_quote : the result of the call to quote
   */
  private[newpricer] def select(
    request:        WakamSubscribe,
    config:         NewPricerConfig,
    selected_quote: Quote
  ): Future[Fail \/ Quote] = {
    for {
      response <- ws.url(s"$wakam_url/subscribe").addHttpHeaders("Authorization" -> config.key).post(
                    Json.toJson(request)(WakamSubscribe.wakam_subscribe_write)
                  )
    } yield {
      response.status match {
        case 200 => \/-(selected_quote)
        case _   => response.json.validate[FailureCase] match {
            case JsSuccess(value, _) => -\/(Fail(value.message))
            case JsError(errors)     => -\/(Fail(
                s"Wakam returned a response with status: ${response.status} along with some unknown error: ${errors.map(_._2).mkString(" ")}"
              ))
          }
      }

    }
  }
}
