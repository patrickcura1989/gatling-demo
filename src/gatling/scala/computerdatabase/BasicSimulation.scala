package computerdatabase

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.duration._
import scalaj.http._
import com.jayway.jsonpath.JsonPath

class BasicSimulation extends Simulation {

  val csvFeeder = csv("getCredits.csv")//.random

  val oauthResponse: HttpResponse[String] =
    Http("test")
      .postForm(Seq(
        "grant_type" -> "client_credentials",
        "client_id" -> "test",
        "scope" -> "test",
        "client_secret" -> "test"
      ))
      .asString

  val accessToken = JsonPath.read[String](oauthResponse.body, "$.access_token")


  val scn = scenario("Get Credits Scenario")
    .feed(csvFeeder)
    //    .exec(http("Get Access Token")
    //      .get("test")
    //      .formParam("grant_type", "client_credentials")
    //      .formParam("client_id", "test")
    //      .formParam("scope", "test")
    //      .formParam("client_secret", "test")
    //      .check(status.is(200))
    //      .check(jsonPath("$.access_token").saveAs("access_token"))
    //    )
    .exec(http("Get Credits")
      .get("test/v1/supplyAgreement/${SupplyAgreementId}/credit")
      //      .header("Authorization", "Bearer ${access_token}")
      .header("Authorization", "Bearer " + accessToken)
      .queryParam("dateTimeFrom","${BookingStart}")
      .queryParam("dateTimeTo","${BookingEnd}")
      .queryParam("loyaltyAccountId","${AccountId}")
      .check(status.is(200))
      .check(regex("\"(csrf)\": \"((\\\\\"|[^\"])*)\"").ofType[(String, String)].saveAs("CSRF_2"))
      .check(regex("\"(transId)\": \"((\\\\\"|[^\"])*)\"").ofType[(String, String)].saveAs("TRANS_ID_2"))
    )
    .exec { session =>
      //println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" + session("CSRF").as[(String, String)]._2 )
      session.set("CSRF_TOKEN_2", session("CSRF_2").as[(String, String)]._2)
        .set("STATE_PROPERTIES_2", session("TRANS_ID_2").as[(String, String)]._2.split("StateProperties=")(1))
      //println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" + session("TRANS_ID").as[(String, String)]._2.split("StateProperties=")(1) )
    }

  setUp(scn.inject(
    atOnceUsers(1)
    //constantUsersPerSec(scala.util.Properties.envOrElse("REQ_PER_SEC", "0.75").toDouble) during (scala.util.Properties.envOrElse("DURATION", "60").toInt seconds)
  ))
}
