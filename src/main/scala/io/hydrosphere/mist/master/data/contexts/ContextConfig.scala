package io.hydrosphere.mist.master.data.contexts

import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions, ConfigValue, ConfigValueFactory, ConfigValueType}
import io.hydrosphere.mist.master.data.{ConfigRepr, NamedConfig}

import scala.collection.JavaConverters._
import scala.concurrent.duration.{Duration, FiniteDuration}

case class ContextConfig(
  name: String,
  sparkConf: Map[String, String],
  downtime: Duration,
  maxJobs: Int,
  precreated: Boolean,
  runOptions: String,
  streamingDuration: Duration
) extends NamedConfig

object ContextConfig {

  implicit val Repr = new ConfigRepr[ContextConfig] {

    val allowedTypes = Set(
      ConfigValueType.STRING,
      ConfigValueType.NUMBER,
      ConfigValueType.BOOLEAN
    )

    override def fromConfig(config: Config): ContextConfig = {
      ContextConfig(
        name = config.getString("name"),
        sparkConf = config.getConfig("spark-conf").entrySet().asScala
          .filter(entry => allowedTypes.contains(entry.getValue.valueType()))
          .map(entry => entry.getKey -> entry.getValue.unwrapped().toString)
          .toMap,
        downtime = Duration(config.getString("downtime")),
        maxJobs = config.getInt("max-parallel-jobs"),
        precreated = config.getBoolean("precreated"),
        runOptions = config.getString("run-options"),
        streamingDuration = Duration(config.getString("streaming-duration"))
      )
    }

    override def toConfig(a: ContextConfig): Config = {
      import ConfigValueFactory._

      def fromDuration(d: Duration): ConfigValue = {
        d match {
          case f: FiniteDuration => fromAnyRef(s"${f.toSeconds}s")
          case inf => fromAnyRef("Inf")
        }
      }
      val map = Map(
        "spark-conf" -> fromMap(a.sparkConf.asJava),
        "downtime" -> fromDuration(a.downtime),
        "max-parallel-jobs" -> fromAnyRef(a.maxJobs),
        "precreated" -> fromAnyRef(a.precreated),
        "run-options" -> fromAnyRef(a.runOptions),
        "streaming-duration" -> fromDuration(a.streamingDuration)
      )
      fromMap(map.asJava).toConfig
    }
  }

}

