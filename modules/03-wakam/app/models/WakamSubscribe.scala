package wakam.home.models

import java.time.OffsetDateTime

private[wakam] final case class WakamSubscribe(
  quote_reference:                   String,
  reference_client:                  String,
  start_date_effect:                 OffsetDateTime,
  end_date_effect:                   OffsetDateTime,
  split_payment:                     String,
  first_due_date:                    OffsetDateTime,
  last_name:                         String,
  first_name:                        String,
  title:                             String,
  email:                             String,
  iban:                              String,
  bic:                               String,
  bank_code:                         String,
  bank_name:                         String,
  holder:                            String,
  mobile_number:                     String,
  address:                           String,
  municipality:                      String,
  postal_code:                       String,
  beneficiaries:                     List[Beneficiaries],
  date_of_birth:                     OffsetDateTime,
  said_place:                        String,
  channel_name:                      String,
  channel_number:                    Int,
  quality:                           String,
  payment_number:                    String,
  payment_amount:                    Int,
  previous_insurer:                  String,
  previous_policy_number:            String,
  previous_policy_subscription_date: OffsetDateTime
)
private[wakam] case class Beneficiaries(
  kind:          String,
  title:         String,
  last_name:     String,
  first_name:    String,
  date_of_birth: OffsetDateTime
)
