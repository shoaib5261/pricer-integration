package new_pricer.new_vertical

import domain._
import helpers.sorus.Fail
import helpers.sorus.SorusDSL.Sorus
import new_pricer.new_vertical.models.{ NewPricerConfig, NewPricerRequest }
import new_pricer.services.NewPricerService
import play.api.Logging
import play.api.libs.json._
import scalaz.\/

import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future

@Singleton
class NewPricerNewVertical @Inject() (
  service: NewPricerService
) extends PricerService
    with Sorus
    with Logging {

  def input_quote_format(pricer_id: String): List[InputFormat] = {
    InputFormatFactoryNewPricerNewVertical.input_format_quote
  }

  def input_select_format(pricer_id: String): List[InputFormat] = {
    InputFormatFactoryNewPricerNewVertical.input_format_select
  }

  def quote(broker_config: Option[JsValue])(
    pricer_id:             String,
    quote_input:           QuoteInput
  ): Future[Fail \/ PricerResponse] = {
    logger.info(s"[newPricer] [quote] The pricer request is  ${(quote_input.input_json)}")
    for {
      pricer_request <- quote_input.input_json.validate[NewPricerRequest] ?| ()
      auth           <- broker_config                                     ?| "error.broker.login.required" // defined in I18n in the directory newpricer
      broker_config  <- auth.validate[NewPricerConfig]                    ?| ()
      result         <- service.quote(pricer_request, broker_config)      ?| ()
    } yield {
      result
    }
  }

  def select(broker_config: Option[JsValue])(
    pricer_id:              String,
    subscription_input:     SelectSubscriptionInput
  ): Future[Fail \/ SelectSubscriptionOutput] = {
    logger.info(s"[newPricer] [select] The pricer request is  ${(subscription_input.data)}")
    for {
      pricer_request <- subscription_input.data.validate[NewPricerRequest] ?| ()
      select_data    <- pricer_request.select_data                         ?| "select data for new_pricer new_vertical is empty"
      auth           <- broker_config                                      ?| "error.broker.login.required"
      broker_config  <- auth.validate[NewPricerConfig]                     ?| ()
      updated_quote  <- service.select(
                          pricer_request,
                          broker_config,
                          select_data // put select data directly avoid to carry option during  service part
                        ) ?| ()
    } yield {
      SelectSubscriptionOutput("200", updated_quote)
    }
  }
}
