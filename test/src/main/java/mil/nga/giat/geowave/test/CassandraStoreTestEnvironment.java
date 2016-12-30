package mil.nga.giat.geowave.test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.cassandra.AbstractCassandraMojo;
import org.codehaus.mojo.cassandra.StartCassandraClusterMojo;
import org.codehaus.mojo.cassandra.StopCassandraClusterMojo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mil.nga.giat.geowave.core.store.DataStore;
import mil.nga.giat.geowave.core.store.GenericStoreFactory;
import mil.nga.giat.geowave.core.store.StoreFactoryOptions;
import mil.nga.giat.geowave.core.store.util.ClasspathUtils;
import mil.nga.giat.geowave.datastore.cassandra.CassandraDataStoreFactory;
import mil.nga.giat.geowave.datastore.cassandra.operations.config.CassandraRequiredOptions;
import mil.nga.giat.geowave.test.annotation.GeoWaveTestStore.GeoWaveStoreType;

public class CassandraStoreTestEnvironment extends
		StoreTestEnvironment
{
	private final static Logger LOGGER = LoggerFactory.getLogger(
			CassandraStoreTestEnvironment.class);

	private static CassandraDataStoreFactory STORE_FACTORY;
	private static CassandraStoreTestEnvironment singletonInstance = null;
	protected static final File TEMP_DIR = new File(
			"./target/cassandra_temp/");

	private static class StartGeoWaveCluster extends
			StartCassandraClusterMojo
	{
		public StartGeoWaveCluster() {
			super();
			startWaitSeconds = 180;
			rpcAddress = "127.0.0.1";
			rpcPort = 9160;
			jmxPort = 7199;
			startNativeTransport = true;
			nativeTransportPort = 9042;
			listenAddress = "127.0.0.1";
			storagePort = 7000;
			stopPort = 8081;
			stopKey = "cassandra-maven-plugin";
			maxMemory = 512;
			cassandraDir = TEMP_DIR;
			cassandraDir.mkdirs();
			project = new MavenProject();
			project.setFile(
					cassandraDir);
			Field f;
			try {
				f = StartCassandraClusterMojo.class.getDeclaredField(
						"clusterSize");
				f.setAccessible(
						true);
				f.set(
						this,
						4);
				f = AbstractCassandraMojo.class.getDeclaredField(
						"pluginArtifact");

				f.setAccessible(
						true);
				final DefaultArtifact a = new DefaultArtifact(
						"group",
						"artifact",
						VersionRange.createFromVersionSpec(
								"version"),
						null,
						"type",
						null,
						new DefaultArtifactHandler());
				a.setFile(
						cassandraDir);
				f.set(
						this,
						a);

				f = AbstractCassandraMojo.class.getDeclaredField(
						"pluginDependencies");
				f.setAccessible(
						true);
				f.set(
						this,
						new ArrayList<>());
			}
			catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException
					| InvalidVersionSpecificationException e) {
				LOGGER.error(
						"Unable to initialize start cassandra cluster",
						e);
			}
		}

		@Override
		protected void createCassandraJar(
				File jarFile,
				String mainClass,
				File cassandraDir )
				throws IOException {
			ClasspathUtils.setupPathingJarClassPath(jarFile, mainClass, this.getClass());
//			super.createCassandraJar(
//					jarFile,
//					mainClass,
//					cassandraDir);
		}

		public void start() {
			try {
				super.execute();
			}
			catch (MojoExecutionException | MojoFailureException e) {
				LOGGER.error(
						"Unable to start cassandra cluster",
						e);
			}
		}

	}

	private static class StopGeoWaveCluster extends
			StopCassandraClusterMojo
	{
		public StopGeoWaveCluster() {
			super();
			rpcPort = 9160;
			stopPort = 8081;
			stopKey = "cassandra-maven-plugin";
			Field f;
			try {
				f = StopCassandraClusterMojo.class.getDeclaredField(
						"clusterSize");
				f.setAccessible(
						true);
				f.set(
						this,
						4);
				f = StopCassandraClusterMojo.class.getDeclaredField(
						"rpcAddress");

				f.setAccessible(
						true);
				f.set(
						this,
						"127.0.0.1");
			}
			catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				LOGGER.error(
						"Unable to initialize stop cassandra cluster",
						e);
			}
		}

		public void stop() {
			try {
				super.execute();
			}
			catch (MojoExecutionException | MojoFailureException e) {
				LOGGER.error(
						"Unable to stop cassandra cluster",
						e);
			}
		}
	}

	public static synchronized CassandraStoreTestEnvironment getInstance() {
		if (singletonInstance == null) {
			singletonInstance = new CassandraStoreTestEnvironment();
		}
		return singletonInstance;
	}

	private CassandraStoreTestEnvironment() {}

	@Override
	protected void initOptions(
			final StoreFactoryOptions options ) {
		final CassandraRequiredOptions cassandraOpts = (CassandraRequiredOptions) options;
		cassandraOpts.setContactPoint(
				"127.0.0.1");
	}

	@Override
	protected GenericStoreFactory<DataStore> getDataStoreFactory() {
		return STORE_FACTORY;
	}

	@Override
	public void setup() {
		new StartGeoWaveCluster().start();
	}

	@Override
	public void tearDown() {
		new StopGeoWaveCluster().stop();
	}

	@Override
	protected GeoWaveStoreType getStoreType() {
		return GeoWaveStoreType.CASSANDRA;
	}

	@Override
	public TestEnvironment[] getDependentEnvironments() {
		return new TestEnvironment[] {};
	}
}
