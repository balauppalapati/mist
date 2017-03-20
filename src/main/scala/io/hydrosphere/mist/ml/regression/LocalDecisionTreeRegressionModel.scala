package io.hydrosphere.mist.ml.regression

import io.hydrosphere.mist.lib.{LocalData, LocalDataColumn}
import io.hydrosphere.mist.ml.{DataUtils, LocalModel, LocalTypedTransformer, Metadata}
import org.apache.spark.ml.linalg.{SparseVector, Vector}
import org.apache.spark.ml.regression.DecisionTreeRegressionModel
import org.apache.spark.ml.tree.Node
import org.apache.spark.ml.linalg.{DenseVector, SparseVector, Vector}
import org.apache.spark.mllib.linalg.{DenseMatrix => OldDenseMatrix, DenseVector => OldDenseVector, Matrices => OldMatrices, SparseVector => SVector, Vector => OldVector, Vectors => OldVectors}

object LocalDecisionTreeRegressionModel extends LocalTypedTransformer[DecisionTreeRegressionModel] {
  override def localLoad(metadata: Metadata, data: Map[String, Any]): DecisionTreeRegressionModel = {
    createTree(metadata, data)
  }

  def createTree(metadata: Metadata, data: Map[String, Any]): DecisionTreeRegressionModel = {
    val ctor = classOf[DecisionTreeRegressionModel].getDeclaredConstructor(classOf[String], classOf[Node], classOf[Int])
    ctor.setAccessible(true)
    val inst = ctor.newInstance(
      metadata.uid,
      DataUtils.createNode(0, metadata, data),
      metadata.numFeatures.get.asInstanceOf[java.lang.Integer]
    )
    inst.setFeaturesCol(metadata.paramMap("featuresCol").asInstanceOf[String])
      .setPredictionCol(metadata.paramMap("predictionCol").asInstanceOf[String])
  }

  override def transformTyped(tree: DecisionTreeRegressionModel, localData: LocalData): LocalData = {
    localData.column(tree.getFeaturesCol) match {
      case Some(column) =>
        val method = classOf[DecisionTreeRegressionModel].getMethod("predict", classOf[Vector])
        method.setAccessible(true)
        val newColumn = LocalDataColumn(tree.getPredictionCol, column.data map { feature =>
          val vector: SparseVector = feature match {
            case v: SparseVector => v
            case v: SVector => DataUtils.mllibVectorToMlVector(v)
            case x => throw new IllegalArgumentException(s"$x is not a vector")
          }
          method.invoke(tree, vector).asInstanceOf[Double]
        })
        localData.withColumn(newColumn)
      case None => localData
    }
  }
}
