package typedapi.server

import shapeless._
import shapeless.ops.hlist.Prepend

import scala.language.higherKinds
import scala.annotation.implicitNotFound

@implicitNotFound("""Cannot find EndpointExecutor. Do you miss some implicit values e.g. encoder/decoder?

elements: ${El}
input keys: ${KIn}
input values: ${VIn}""")
sealed trait EndpointExecutor[El <: HList, KIn <: HList, VIn <: HList, ROut, F[_], FOut] {

  type R
  type Out

  def extract(eReq: EndpointRequest, endpoint: Endpoint[El, KIn, VIn, ROut, F, FOut]): Either[ExtractionError, ROut] = 
    endpoint.extractor(eReq, Set.empty, HNil)

  def apply(req: R, eReq: EndpointRequest, endpoint: Endpoint[El, KIn, VIn, ROut, F, FOut]): Either[ExtractionError, Out]
}

object EndpointExecutor {

  type Aux[R0, El <: HList, KIn <: HList, VIn <: HList, ROut, F[_], FOut, Out0] = EndpointExecutor[El, KIn, VIn, ROut, F, FOut] {
    type R = R0
    type Out = Out0
  }
}

trait NoReqBodyExecutor[El <: HList, KIn <: HList, VIn <: HList, F[_], FOut] extends EndpointExecutor[El, KIn, VIn, VIn, F, FOut] {

  protected def execute(input: VIn, endpoint: Endpoint[El, KIn, VIn, VIn, F, FOut]): F[FOut] = 
    endpoint.apply(input)
}

trait ReqBodyExecutor[El <: HList, KIn <: HList, VIn <: HList, Bd, ROut <: HList, POut <: HList, F[_], FOut] extends EndpointExecutor[El, KIn, VIn, (BodyType[Bd], ROut), F, FOut] {

  implicit def prepend: Prepend.Aux[ROut, Bd :: HNil, POut]
  implicit def eqProof: POut =:= VIn

  protected def execute(input: ROut, body: Bd, endpoint: Endpoint[El, KIn, VIn, (BodyType[Bd], ROut),  F, FOut]): F[FOut] = 
    endpoint.apply(input :+ body)
}
