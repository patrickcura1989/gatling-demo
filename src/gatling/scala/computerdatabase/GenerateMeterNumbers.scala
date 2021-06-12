package computerdatabase

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration._

class GenerateMeterNumbers extends Simulation {

  val csvFeeder = csv("meters.csv")

  //Java Writer
  val validMetersWriter = {
    val fos = new java.io.FileOutputStream("validMeters.csv")
    new java.io.PrintWriter(fos,true)
  }

  val scn = scenario("Get Supply Agreements - V1")
    .feed(csvFeeder)
    .exec { session =>
      session.set("include", "null")
    }
    .exec(http("Get Supply Agreements - V1")
      .get("test${CONSUMERNO}/supplyPoints/ELECTRICITY-${ICP}/meters/${SERIAL}/devices?include=consumption&filter%5Bconsumption.productCharge.date%5D=LATEST&fields%5BConsumption%5D=consumptionEndDate,reading%0A")
      .header("x-Application-ID", "MYACC")
      .header("x-Channel-ID", "Gatling")
      .header("x-Brand-ID", "GENE")
      .header("Content-Type", "application/json")
      .header("client_id", "test")
      .header("client_secret", "test")
      .check(status.is(200))
      .check(jsonPath("$..data.devices[0]").exists.saveAs("include"))
    )
    .exec { session =>
      if(!session("include").as[String].equals("null"))
      {
        validMetersWriter.println(session("CONSUMERNO").as[String] + "," + session("ICP").as[String] + "," + session("SERIAL").as[String])
      }
      session
    }

  setUp(scn.inject(
    //atOnceUsers(1)
    constantUsersPerSec(0.65) during (10800 seconds)
    //constantUsersPerSec(scala.util.Properties.envOrElse("REQ_PER_SEC", "0.75").toDouble) during (scala.util.Properties.envOrElse("DURATION", "60").toInt seconds)
  ))
}
