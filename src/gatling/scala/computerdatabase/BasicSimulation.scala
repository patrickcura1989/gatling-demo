package computerdatabase

import com.fasterxml.jackson.databind.JsonNode
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.duration._
import scalaj.http._
import com.jayway.jsonpath.JsonPath
import io.gatling.core.check.MultipleFindCheckBuilder
import io.gatling.core.check.jsonpath.{JsonPathCheckBuilder, JsonPathCheckType, JsonPathOfType, JsonPaths}
import io.gatling.core.session.Expression
import java.io.BufferedWriter
import java.io.FileWriter
import spray.json._
import DefaultJsonProtocol._

class BasicSimulation extends Simulation {

  val date = "2021-06-18"

  val validDataWriter = {
    //val fos = new java.io.FileOutputStream("validData"+date+".csv")
    val fos = new java.io.FileOutputStream("validData.csv", true)
    new java.io.PrintWriter(fos,true)
  }

  val urlresponse: HttpResponse[String] =
    Http("https://api.nasdaq.com/api/calendar/dividends?date="+date)
      .header("Cookie","ak_bmsc=08991DDE4C59A58F5F84CACA57EA3C74686D3415C7290000E2E7C3603BFB4652~pl/DWctTPquudexe1zm87P0rvsq2umB/uVh5/jK+auVDv38AbV5w7tqamoZVR/aD71sFZv7EV1PS4kcxHXzG+NTrT+O91vaqopwFOSRik0vHgQKdyn4Ios/g97ArwHGLL+YhIEHPUu6Ya9PyuHzLlAPCQth7egOubJBH4x+je2srajmWoVs3l4TmHVmtfRCRGW0u0aQ8iU3M+IV4/SIx37eGAABU6UZu5TAiqRTpgSNHg=; bm_sv=29508F798455E97665A266158F610CE6~4CGi8oNdXobWpFpkrFT1NMQt8qzxmi0LE0fa375b45orc+G4bpnxzqnbK7W5Z03BG9EHx1fJOiQ11GBGi7ERbxMamr+/lHm/V/j6AO/+shlivV/xVzYjWenCFyohPe0CoQSIWt/hoASdYS2hEJiDX+ABAB5hRQq+4kXGuK6Fky0=; NSC_W.OEBR.DPN.7070=ffffffffc3a0f73345525d5f4f58455e445a4a422dae")
      .asString
  val jsonAst = urlresponse.body.parseJson
  val data = jsonAst.asJsObject().fields("data").asJsObject().fields("calendar").asJsObject().fields("rows").toString()
  val file = new FileWriter("feeder.json")
  val output = new BufferedWriter(file)
  output.write(data)
  output.close
  val jsonFileFeeder = jsonFile("feeder.json")

  val scn = scenario("Generate Dividend Data")
    .feed(jsonFileFeeder)
    .exec(http("Get Stock Details")
      .get("https://api.nasdaq.com/api/quote/${symbol}/info?assetclass=stocks")
      .header("Cookie","ak_bmsc=31EB54A1C899D6DBE941F78F34C65396686D3415C7290000C35BC4601BBE2D72~plFVKBKBdLTZvwMAlHAgEsKguOfZaGtaZXCWZe7FbzVj7TGJiSL9L0f1DQxpulcnRbmJ/TsWXjsyLO/cEjWQKAtcPO3DTs9/9B2KBD5YLbXimpZX5ukE2T17Tf7royfzP6F2ye7pThECFUAVPfqLXYQrgp0t59iAmJ3VxyfQF6KkKIWPgf3xXDhNGhWZzoSEaSRD2iBQxszMm1YrfeE2MekVTGJFN6gbCGg34tslUFHnE=; bm_sv=A228E33C294E2CAE96CA09EF456C19B8~4CGi8oNdXobWpFpkrFT1NHSQvLqfodjjbyAAMLWNeIz9ZKlfF/ER3HDrBcfz0iNmR2K3OGamlphzJiGhvutfQoPgntB8eDUJS8EFI/P0aUdBx08pUbhBN5l6cGmUGiy0t+gW05vHSRNi3UAvTb7sx/3lNmfBGnCFQP+G78LzsD4=")
      .check(status.is(200))
      .check(jsonPath("$.data.keyStats.PreviousClose.value").saveAs("value"))
    )
    .exec { session =>
      validDataWriter.println(
        session("symbol").as[String] + ","
          + 10.0/session("value").as[String].replace("$","").toFloat + ","
          + session("dividend_Ex_Date").as[String] + ","
          + 10.0/session("value").as[String].replace("$","").toFloat * session("dividend_Rate").as[String].toFloat + ","
          + session("value").as[String].replace("$","") + ","
          + session("dividend_Rate").as[String]
      )
      session
    }

  setUp(scn.inject(
    //atOnceUsers(1)
    constantUsersPerSec(scala.util.Properties.envOrElse("REQ_PER_SEC", "0.75").toDouble) during (scala.util.Properties.envOrElse("DURATION", "3600").toInt seconds)
  ))
}
