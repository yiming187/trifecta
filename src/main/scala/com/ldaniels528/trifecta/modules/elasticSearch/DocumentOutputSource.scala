package com.ldaniels528.trifecta.modules.elasticSearch

import com.ldaniels528.trifecta.support.avro.AvroDecoder
import com.ldaniels528.trifecta.support.elasticsearch.TxElasticSearchClient
import com.ldaniels528.trifecta.support.io.{KeyAndMessage, OutputSource}
import com.ldaniels528.trifecta.support.messaging.MessageDecoder
import com.ldaniels528.trifecta.util.TxUtils._
import com.ning.http.client.Response

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * Elastic Search Document Output Source
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class DocumentOutputSource(client: TxElasticSearchClient, index: String, indexType: String, id: Option[String])
  extends OutputSource {

  /**
   * Writes the given key and decoded message to the underlying stream
   * @param data the given key and message
   * @return the response value
   */
  override def write(data: KeyAndMessage, decoder: Option[MessageDecoder[_]])(implicit ec: ExecutionContext): Future[Response] = {
    decoder match {
      case Some(av: AvroDecoder) =>
        av.decode(data.message) match {
          case Success(record) =>
            val myId = (id ?? (Option(data.key) map (new String(_, encoding)))) getOrElse (throw new IllegalStateException("No ID specified"))
            client.create(index, indexType, myId, record.toString)
          case Failure(e) =>
            throw new IllegalStateException(e.getMessage, e)
        }
      case Some(unhandled) =>
        throw new IllegalStateException(s"Unhandled decoder '$unhandled'")
      case None =>
        throw new IllegalStateException(s"No message decoder specified")
    }
  }

  override def close(): Unit = ()

}
