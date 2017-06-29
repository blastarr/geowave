package mil.nga.giat.geowave.analytic.javaspark;

import java.io.IOException;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.clustering.KMeans;
import org.apache.spark.mllib.clustering.KMeansModel;
import org.apache.spark.mllib.linalg.Vector;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mil.nga.giat.geowave.core.store.operations.remote.options.DataStorePluginOptions;
import mil.nga.giat.geowave.mapreduce.input.GeoWaveInputKey;

public class KMeansRunner
{
	private final static Logger LOGGER = LoggerFactory.getLogger(KMeansRunner.class);
	
	private String appName = "KMeansRunner";
	private String master = "local";
	private String host = "localhost";

	private JavaSparkContext jsc;
	private DataStorePluginOptions inputDataStore = null;
	private JavaRDD<Vector> centroidVectors;
	private KMeansModel outputModel;

	private int numClusters = 8;
	private int numIterations = 20;
	private double epsilon = -1.0;

	public KMeansRunner() {
	}

	private void initContext() {
		SparkConf sparkConf = new SparkConf();

		sparkConf.setAppName(appName);
		sparkConf.setMaster(master);
		
		// TODO: other context config settings
		
		jsc = new JavaSparkContext(
				sparkConf);
	}

	public void run()
			throws IOException {
		initContext();
		
		// Validate inputs
		if (inputDataStore == null) {
			LOGGER.error("You must supply an input datastore!");
			throw new IOException(
					"You must supply an input datastore!");
		}

		// Load RDD from datastore
		JavaPairRDD<GeoWaveInputKey, SimpleFeature> javaPairRdd = GeoWaveRDD.rddForSimpleFeatures(
				jsc.sc(),
				inputDataStore);

		// Retrieve the input centroids
		centroidVectors = GeoWaveRDD.rddFeatureVectors(javaPairRdd);
		centroidVectors.cache();

		// Init the algorithm
		KMeans kmeans = new KMeans();
		kmeans.setInitializationMode("kmeans||");
		kmeans.setK(numClusters);
		kmeans.setMaxIterations(numIterations);

		if (epsilon > -1.0) {
			kmeans.setEpsilon(epsilon);
		}

		// Run KMeans
		outputModel = kmeans.run(centroidVectors.rdd());
	}

	public JavaRDD<Vector> getInputCentroids() {
		return centroidVectors;
	}

	public DataStorePluginOptions getInputDataStore() {
		return inputDataStore;
	}

	public void setInputDataStore(
			DataStorePluginOptions inputDataStore ) {
		this.inputDataStore = inputDataStore;
	}

	public void setNumClusters(
			int numClusters ) {
		this.numClusters = numClusters;
	}

	public void setNumIterations(
			int numIterations ) {
		this.numIterations = numIterations;
	}

	public void setEpsilon(
			Double epsilon ) {
		this.epsilon = epsilon;
	}

	public KMeansModel getOutputModel() {
		return outputModel;
	}

	public void setAppName(
			String appName ) {
		this.appName = appName;
	}

	public void setMaster(
			String master ) {
		this.master = master;
	}

	public void setHost(
			String host ) {
		this.host = host;
	}
}
