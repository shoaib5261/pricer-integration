Pricer Integration
===========================================

# Intro

The main goal of the application is to display insurance's offers and allow the user to select one of them.

The application allow

* to query insurance's webservice that return a price and some data about the insurance. We call this endpoint `quote`
* to fulfill a contract with an insurer by providing data (name, address, etc...). We call this endpoint `select` because the end-user have selected an insurance

# Setup dev environment

## Requirement

* Java 11
* sbt 1.6.2
* scala 2.13.8

## Setup

You need to clone the repository.
```
git clone https://github.com/Particeep/pricer-integration.git
```
Or in ssh :
```
git clone git@github.com:Particeep/pricer-integration.git
```

Then it's a standard sbt project
```
cd pricer-integration
sbt clean update compile
```

# Sbt Structure of the project

The base application is a standard [Play](https://www.playframework.com/) application

There are 3 sbt modules

* `01-core` is commons code that is available for convenience. You will probably use StringUtils or DateUtils
* `02-domain` is the domain of the application. It defines input and output type for the part you need to implement
* `03-new-pricer` is the module you need to implement.

# Your Goal

The goal is to complete the module of `/modules/03-newpricer` with code that implement the pricer you have been assigned.

Especially this parts that contains un-implemented method

* `InputFormatFactoryNewPricerNewVertical.input_format_quote`
* `InputFormatFactoryNewPricerNewVertical.input_format_select`
* `NewPricerNewVertical.quote`
* `NewPricerNewVertical.select`

And un-implemented case class 
* QuoteData
* SelectData
* NewPricerConfig

# InputFormat

You need to implement 2 InputFormat

* `InputFormatFactoryNewPricerNewVertical.input_format_quote`
* `InputFormatFactoryNewPricerNewVertical.input_format_select`

InputFormat is a schema that describes json in a way that suit our need.
The building bloc are defined in the domain module

* `input_format_quote` describe the json input of the `quote` endpoint
* `input_format_select` describe the json input of the `select` endpoint, minus every fields that are already defined in `input_format_quote`

## Basic structure

```scala
case class InputFormat(
  // The name of the field, like the name of a field in an HTML form. It's the unique identifier of the field
  name:             String,

  // Type of the field : TEXT, DATE, NUMBER etc...
  kind:             FieldType,

  // Is the field required ?
  mandatory:        Boolean,

  // If kind == ENUM then this list the values that are allowed
  options:          List[String]              = List(),

  // Can we select multiple values
  multiple:         Boolean                   = false,

  // Is the field's value an array ?
  is_array:         Boolean                   = false,

  // If kind == OBJECT, we have a nested structure with sub-fields
  fields:           Option[List[InputFormat]] = None,

  // General tagging
  tags:             Option[List[String]]      = None,

  // Internal use, you don't need this
  external_service: Option[ExternalService]   = None
)
```

NB 1: if `is_array` is true then the json type is an array and items in the array are of type `kind`

NB 2: if `is_array` is false and `multiple` is true then is the case where it create a checkbox, and fields  defined by data in attribute "option".

## Sample Code

```scala
  val insureds_format = {
    InputFormat(
      name      = "assures",
      kind      = OBJECT,
      mandatory = true,
      is_array  = true,
      fields    = Some(
        List(
          civility_format,
          firstname_format,
          lastname_format,
          birthdate_format,
          smoker_format
        )
      )
    )
  }

  private[this] def civility_format: InputFormat = {
    InputFormat(name = "civility", kind = ENUM, mandatory = true, options = List("MR", "MME", "MISS"))
  }

  private[this] def firstname_format: InputFormat = {
    InputFormat(name = "first_name", kind = TEXT, mandatory = true)
  }

  private[this] def lastname_format: InputFormat = {
    InputFormat(name = "last_name", kind = TEXT, mandatory = true)
  }

  private[this] def birthdate_format: InputFormat = {
    InputFormat(name = "birthdate", kind = DATE, mandatory = true)
  }

  private[this] def smoker_format: InputFormat = {
    InputFormat(name = "is_smoking", kind = BOOLEAN, mandatory = false)
  }
```

# Quote endpoint

You have to implement the method in `NewPricerService.quote`.
```scala
def quote(
    request: NewPricerRequest,
    config:  NewPricerConfig
  ): Future[Fail \/ PricerResponse] = {
    ???
  }
```

## Input

The input type `NewPricerRequest` and `NewPricerConfig` should be defined by you according to the requirement of the webservice you're working on.
They should reflect the input format you define.

## Output

* `\/` is the [scalaz disjunction](https://eed3si9n.com/learning-scalaz/Either.html), it works almost like the standard `Either`
* `Fail` is an error type, defined in the project. It's a wrapper on a String and a stacktrace.

### PricerResponse
This is the type that you have to return to the method quote.

#### PricerError

```scala
case class PricerError(message: String, args: List[String] = List.empty) extends PricerResponse
```

`PricerError` is use when insurer API return a business error.
A business error is a case where the insurer refuses the user due to an inconsistency in the data.

For instance driver age is inferior to 18. Another example you can not say you have a good vision but at the same time you are blind.

Unfortunately, api insurer can make difficult how to separate technical problem from business problem. In this case you can return a sentence like "you are not eligible"
and put on a `Fail` what the api insured return.

A technical problem is an error on data. For instance, they are missing data or data is not understood by the api insurer.
That implied you made a mistake in the code, and you have to correct it.

Insurer API can send respond in english or in French. You do not have to translate.

#### Decline

```scala
case class Decline(url: URL, meta: Option[Meta] = None) extends PricerResponse
```
`Decline` is use when insurer API refuse to give you a price because your profile is not eligible. 
All data from user are good and consistent, but Insurer API does not want to insure this user profile.
At this moment you have to put the reason and use this class. It can be for instance :
```
There are no offer for you in our database accoring to your profile : you did too much infraction this year.
```
As you can see, there is the type `Meta` and `URL`. Ignore `URL`.

`Meta` is a type which structure text.
```scala
case class Meta(
  title:       Option[String]             = None,
  sub_title:   Option[String]             = None,
  description: Option[String]             = None,
  documents:   Option[List[MetaDocument]] = None,
  args:        Map[String, List[String]]  = Map.empty
)
```
In general, you will use title and description. You will use the rest of the attribute if we specific to you this requirement.
Attributes title, sub_title and description are subject to I18n, so you need to declare in `message.en.conf` and `message.fr.conf` in newpricer directory fields and value and call them.

Example : 

In `message.en.conf` we have this :
````
newpricer.title.decline = "Quote denied"
newpricer.title.description = "you are not eligible"
````
And in `message.fr.conf` we have this :
````
newpricer.title.decline = "Tarification refusée"
newpricer.title.description = "Vous n'êtes pas éligble."
````
your `Meta` will be : 
```scala
val meta : Meta = Meta(title = "newpricer.title.decline".some, description = "newpricer.title.description".some )
```

#### Offer

```scala
case class Offer(
                  price:         Price,
                  detail:        List[OfferItem]      = List(),
                  internal_data: Option[InternalData] = None,
                  external_data: Option[JsObject]     = None,
                  meta:          Option[Meta]         = None
                ) extends PricerResponse
```
The type `Offer` is use when insurer API give you a price. This is a success caseThis type is always with the type `Price`, but we will see it later.
Overall, you have to put data according to what we demand. You will have to put :

##### Amount

For us, a price that insurer API give you, is of type `Amount`. All monetary amounts are in cents.

```scala
case class Amount(value: Int)
```
Example :
insurer API return a price and the price 123.34, you have to do :
```scala
import utils.NumberUtils
val result_request : Double = 123.34 // res0 : 123.34
val amount : Amount = Amount(NumberUtils.amountFromDoubleToCentime(result_request)) // res1 : Amount(12334)
```
##### Price

```scala
case class Price(
                  amount_ht:   Amount,
                  owner_fees:  Amount    = Amount(0),
                  broker_fees: Amount    = Amount(0),
                  taxes:       Amount    = Amount(0),
                  currency:    Currency  = Currency.getInstance("EUR"),
                  frequency:   Frequency = Frequency.ONCE
                )
```
You will have to feed each attribute according to what we demand.

Example:

Imagine you have the doc and we write that
```scala
currency : EUR
owner_fees : 0€
taxes : 1337,21€
broker_fees :  234€
```
And the price that insurer API give you, is 12€. So you need to feed your type price like that
```scala
val price : Price = Price(
    amount_ht   = Amount(NumberUtils.amountFromDoubleToCentime(12)),
    taxes       = Amount(NumberUtils.amountFromDoubleToCentime(1337.21)),
    broker_fees = Amount(234)
  )
```
##### OfferItem

```scala
case class OfferItem(
                      label: String,
                      value: String,
                      kind: String,
                      args: List[String] = List.empty
                    )
```
`OfferItem` is additional data which come with the price. What put in this is an information that we will give you if needed.

Parameter kind is the type of your data which can be defined by us. 

This case class is subject to I18n and like `Meta` so if needed, you have to define in message.en.newpricer.conf and
message.fr.newpricer.conf label and value.

There are something new compared to `Meta` :  the parameter args. Indeed, you can create in message.en.newpricer.conf and
message.fr.newpricer.conf a message with a symbol in order to be replaced. For instance in message.en.conf you can have : 
```
message_with_custom_things = "You have {0} in your wallet and {1} in your hand
```
```scala
val custom_args = List("pistol", "chewing-um")
val offer_item = OfferItem(label = "message_with_custom_things", kind = "text" ,value = "something else", args = custom_args)
// the message will be "You have pistol in your wallet and chewing-um in your hand"
```
Now get back to type `Offer` 
```scala
case class Offer(
                  price:         Price,
                  detail:        List[OfferItem]      = List(),
                  internal_data: Option[InternalData] = None,
                  external_data: Option[JsObject]     = None,
                  meta:          Option[Meta]         = None
                ) extends PricerResponse
```
##### external_data

external_data is data that you need in order to complete select part. You are free to decide JSON structure.
If during the select insurer return you a link, you have to create an external_data.

external_data will be passed identically between quote and select
It is used to transmit data necessary for the call a select as a specific id


# Select endpoint

You should implement the method in `NewPricerService.select`

```scala
private[newpricer] def select(
    request:        NewPricerRequest,
    config:         NewPricerConfig,
    selected_quote: Quote
  ): Future[Fail \/ Quote] = {
    ???
  }
```
## Input

This method take in input new data to send in select part. Depending on insurer you have to send again data quote but in another endpoint, 
and with that new data like name, first name, phone number etc..

## Output

Depending on insurer you can have many cases : 
- what you send is data for user sign up, so you don't have to in more
- insurance send you back (so after you send to it data) a link in order to allow user to continue the process on the insurance site and in this case you have to put
the link in external data

# Fail and ?|

for more detail, see this article : https://medium.com/@adriencrovetto/error-handling-in-scala-with-play-framework-130034b21b37

# General rules

## Renaming

rename newpricer with the real name of the pricer you will work on

Ex: you work on a home insurance for Axa home
You should do these kins of renaming

* `03-new_prier` -> `03-axa`
* `package newpricer.xxx` -> `package axa.home.xxx`
* `NewPricer` in class / function name would become `AxaHome`

# Coding standard

* no `var`, no `null`
* do not add library without consulting us first
* use [OffsetDateTime](https://docs.oracle.com/javase/8/docs/api/java/time/OffsetDateTime.html) for date
* don't throw exception : use `Fail \/ A` to catch error
* don't use try/catch
* be non-blocking : never do blocking in the main execution context or in the play's execution context cf. [play's doc](https://www.playframework.com/documentation/2.8.x/ThreadPools)
* clean compilation error and warning (it exists warning that you can't delete. Delete warning only present on your pricer integration.)
* code must be written in snake_case
* code must be written in english
* code in type level and respect functional programming (no side effect, use monad etc..)
* encapsulate each of your class in private[newpricer] except for the three methods implemented by `PricerService`
* encapsulate your code as much as possible, e.g. use private[this] when needed.
* run `sbt scalastyle` and clean warning
* run `sbt fmt`

# error management and log

- All technical error must be contained on a Fail.
- All business error must be contained on a `PricerError` (when it is possible).

# Testing

It is important to test what you did. The main aims is to test program behavior

You will have to test many cases :
- test case when you received a price
- test case when your select is ok
- test case when pricer return technical error
- test case when pricer return business error

You do not need to test library you use 
(for instance do not create a test which examine if ws client send data or if format json from jsonx.formatCaseClass works).
You do not need to re test your tools, test only what you created.

When you have to test quote part, do not write a test which compare a price in the code with what api insurer give you.
Because an insurer does not have fix price it can change when he wants. You can compare then the AST that you have defined
and see if during the test you receive the good type which carry price.

For instance, we can consider this AST
~~~scala
sealed trait NewPricerNewResponse

final case class NewPricerPrice(value : Double) extends NewPricerNewResponse
final case class NewPricerError(code_error : Int, reason  : String) extends NewPricerNewResponse
~~~

So the test can be 
~~~scala
// https://www.scalatest.org/
"NewPriceService test" should {
  "Success, because it return a price" in {
    val newpricer_service = app.injector.instanceOf[NewPricerService]
    val result = await(newpricer_service.get_price(data, config, TypeOfFormula.BASIC))

    // https://www.scalatest.org/user_guide/using_assertions
    result match {
      case \/-(NewPricerPrice(_)) => succeed
      case not_expected => fail(s"Error during get price -> $not_expected")
    }
  }
}
~~~

If you create method that transform data you have to write test. For instance, insurer want to get date into timestamps unix.
So you have to create an object like this :
~~~scala
import java.time.OffsetDateTime

object NewPricerUtils {
  def to_timestamps_unix(date: OffsetDateTime): String = date.toInstant.getEpochSecond
}
~~~
You need to test this method. In directory test you have to write a new class called for instance "NewPricerMethodTest.scala" and the method with an expected result

~~~scala
"NewPricer new vertical format_date method" should {
  "succeed when formatting date" in {
    val date_test = OffsetDateTime.now(ZoneOffset.UTC).withNano(0)
    val date_to_timestamp: Long = to_timestamps_unix(date_test)
    
   val  revert_date_test_to_offsetdatetime = new Date(date_to_timestamp * 1000).toInstant.atOffset(ZoneOffset.UTC)
    
    date_test.compareTo(date_to_offsetdatetime) must be(0)
  }
}
~~~