package core.issues

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import issues.issue455.server.akkaHttp.definitions.RecursiveData
import issues.issue455.client.akkaHttp.definitions.{RecursiveData => ClientRecursiveData}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.SpanSugar._
import org.scalatest.{EitherValues, FunSuite, Matchers}
import tests.scalatest.EitherTValues
import cats.instances.future._
import issues.issue455.client.akkaHttp.BooResponse

import scala.concurrent.Future

class Issue455 extends FunSuite with Matchers with EitherValues with EitherTValues with ScalaFutures with ScalatestRouteTest {
  override implicit val patienceConfig = PatienceConfig(10 seconds, 1 second)

  test("akka-http server recursive request body") {
    import issues.issue455.server.akkaHttp.{Handler, Resource}
    import issues.issue455.server.akkaHttp.AkkaHttpImplicits._

    val route = Resource.routes(new Handler {
      override def boo(respond: Resource.booResponse.type)(body: RecursiveData): Future[Resource.booResponse] = {
        Future.successful(respond.OK(body))
      }
    })

    Post("/v1/Boo").withEntity(ContentTypes.`application/json`, Issue455.validJsonEntity) ~> route ~> check {
      response.status shouldBe StatusCodes.OK
      val data = entityAs[RecursiveData]
      data shouldBe Issue455.validRecursiveData
    }
  }

  test("akka-http client recursive request body") {
    import issues.issue455.client.akkaHttp.Client

    Client
      .httpClient(req => Future.successful(HttpResponse().withEntity(ContentTypes.`application/json`, Issue455.validJsonEntity)))
      .boo(Issue455.validClientRecursiveData)
      .rightValue
      .futureValue shouldBe BooResponse.OK(Issue455.validClientRecursiveData)
  }
}

object Issue455 {
  val validJsonEntity: String =
    """
      |{
      |  "id": 10,
      |  "display_name": "parent",
      |  "nesting": {
      |    "id": 11,
      |    "display_name": "child"
      |  }
      |}
      |""".stripMargin

  val validRecursiveData: RecursiveData = RecursiveData(
    id = 10,
    displayName = "parent",
    nesting = Some(
      RecursiveData(
        id = 11,
        displayName = "child",
        nesting = None
      )
    )
  )

  val validClientRecursiveData: ClientRecursiveData = ClientRecursiveData(
    id = 10,
    displayName = "parent",
    nesting = Some(
      ClientRecursiveData(
        id = 11,
        displayName = "child",
        nesting = None
      )
    )
  )
}