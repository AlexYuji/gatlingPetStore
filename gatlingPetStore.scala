
import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class gatlingPetStore extends Simulation {

  private val httpProtocol = http
    .baseUrl("https://petstore.octoperf.com")
    .inferHtmlResources()
    .silentResources
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("pt-BR,pt;q=0.8,en-US;q=0.5,en;q=0.3")
    .doNotTrackHeader("1")
    .upgradeInsecureRequestsHeader("1")
    .userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0")
  
  private val headers_2 = Map(
  		"Origin" -> "https://petstore.octoperf.com"
  )

  val animals = csv("animals.csv").circular	

  private val scn = scenario("gatlingPetStore")
	.repeat(3) {
		exec(
		  http("01 - Home")
			.get("/")
		)
		.pause(1)
		.exec(
		  http("02 - Catalogo")
			.get("/actions/Catalog.action")
			.check(regex("""name="_sourcePage" value=(.+?)"""").saveAs("sourcePage"),
			  regex("""name="__fp" value=(.+?)"""").saveAs("fp"))
		)
		.pause(4)
		.feed(animals)
		.exec(
		  http("03 - buscaAnimal")
			.post("/actions/Catalog.action")
			.headers(headers_2)
			.formParam("keyword", "#{animal}")
			.formParam("searchProducts", "Search")
			.formParam("_sourcePage", "#{sourcePage}")
			.formParam("__fp", "#{fp}")
			.check(regex("""/actions/Catalog\.action\?viewProduct=\&amp;productId=(.+?)"""").saveAs("productId"))
		)
		.pause(1)

		.exec(
		  http("04 - selecionaAnimal")
			.get("/actions/Catalog.action?viewProduct=&productId=#{productId}")
			.check(substring("""<h2>#{animal}</h2>"""))
		)
	}

	setUp(scn.inject(rampUsers(3).during(10))).protocols(httpProtocol)
		.assertions(details("01 - Home").responseTime.max.lt(50),global.failedRequests.percent.is(0))
}
