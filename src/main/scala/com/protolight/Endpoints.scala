package com.protolight

import com.protolight.AffirmationsLibrary.{Affirmation, Paging}
import sttp.tapir.*
import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.ZServerEndpoint
import zio.Task
import zio.ZIO

class Endpoints:
  import Endpoints.*

  val helloServerEndpoint: ZServerEndpoint[AffirmationsLibrary, Any] = pingEndpoint.serverLogicSuccess(_ => ZIO.succeed("pong"))

  val getAffirmationServerEndpoint: ZServerEndpoint[AffirmationsLibrary, Any] =
    getAffirmationEndpoint.serverLogicSuccess(id => AffirmationsLibrary.library.flatMap(_.get(id)))

  val affirmationsListingServerEndpoint: ZServerEndpoint[AffirmationsLibrary, Any] =
    affirmationsListingEndpoint.serverLogicSuccess(pagingO => AffirmationsLibrary.getAll(pagingO, None))

  val createAffirmationServerEndpoint: ZServerEndpoint[AffirmationsLibrary, Any] =
    createAffirmationEndpoint.serverLogicSuccess(id => AffirmationsLibrary.library.flatMap(_.create(id)))

  val updateAffirmationServerEndpoint: ZServerEndpoint[AffirmationsLibrary, Any] =
    updateAffirmationEndpoint.serverLogicSuccess(id => AffirmationsLibrary.library.flatMap(_.update(id)))

  val deleteAffirmationServerEndpoint: ZServerEndpoint[AffirmationsLibrary, Any] =
    deleteAffirmationEndpoint.serverLogicSuccess(id => AffirmationsLibrary.library.flatMap(_.delete(id)))

  val apiEndpoints: List[ZServerEndpoint[AffirmationsLibrary, Any]] = List(
    helloServerEndpoint,
    createAffirmationServerEndpoint,
    getAffirmationServerEndpoint,
    updateAffirmationServerEndpoint,
    deleteAffirmationServerEndpoint,
    affirmationsListingServerEndpoint
  )

  val docEndpoints: List[ZServerEndpoint[AffirmationsLibrary, Any]] = SwaggerInterpreter()
    .fromServerEndpoints(apiEndpoints, "affirmations", "1.0.0")

  // tu potrzebne by było włączenie kind-projector zeby dac to jako type lambde
  type T[+A] = zio.RIO[AffirmationsLibrary, A]

  val prometheusMetrics: PrometheusMetrics[T] = PrometheusMetrics.default[T]()
  val metricsEndpoint: ZServerEndpoint[AffirmationsLibrary, Any] = prometheusMetrics.metricsEndpoint

  val all: List[ZServerEndpoint[AffirmationsLibrary, Any]] = apiEndpoints ++ docEndpoints ++ List(metricsEndpoint)

object Endpoints {
  case class NotFound(what: String)

  val paging: EndpointInput[Option[Paging]] =
    query[Option[Int]]("start")
      .and(query[Option[Int]]("limit"))
      .map(input => input._1.flatMap(from => input._2.map(limit => Paging(from, limit))))(paging =>
        (paging.map(_.from), paging.map(_.limit))
      )

  val pingEndpoint: PublicEndpoint[Unit, Unit, String, Any] = endpoint.get
    .in("ping")
    .out(stringBody)

  val affirmationsListingEndpoint: PublicEndpoint[Option[Paging], Unit, List[Affirmation], Any] = endpoint.get
    .in("affirmation" / "all")
    .in(paging)
    .out(jsonBody[List[Affirmation]])

  val getAffirmationEndpoint: Endpoint[Unit, Long, Unit, Affirmation, Any] = endpoint.get
    .in("affirmation")
    .in(
      query[Long]("id")
        .example(9)
    )
    .out(jsonBody[Affirmation])

  val deleteAffirmationEndpoint: Endpoint[Unit, Long, Unit, Boolean, Any] = endpoint.delete
    .in("affirmation")
    .in(
      query[Long]("id")
        .example(738)
    )
    .out(jsonBody[Boolean])

  val createAffirmationEndpoint: PublicEndpoint[Affirmation, Unit, Affirmation, Any] = endpoint.post
    .in("affirmation")
    .in(
      jsonBody[Affirmation]
        .description("The affirmation to add.")
        .example(Affirmation(738, "Pracuję chętnie, efektywnie i z przyjemnością", "Waldemar Wosiński"))
    )
    .out(jsonBody[Affirmation])

  val updateAffirmationEndpoint: Endpoint[Unit, Affirmation, Unit, Boolean, Any] = endpoint.put
    .in("affirmation")
    .in(
      jsonBody[Affirmation]
        .description("The new state of affirmation. Id must exist already.")
        .example(Affirmation(9, "Jestem niewinny i w porządku, gdy pozwalam innym dokonywać własnych wyborów", "Waldemar Wosiński"))
    )
    .out(jsonBody[Boolean])
}
