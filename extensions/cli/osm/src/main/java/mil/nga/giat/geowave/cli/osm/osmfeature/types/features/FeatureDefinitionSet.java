package mil.nga.giat.geowave.cli.osm.osmfeature.types.features;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mil.nga.giat.geowave.adapter.vector.FeatureDataAdapter;
import mil.nga.giat.geowave.cli.osm.osmfeature.FeatureConfigParser;
import mil.nga.giat.geowave.cli.osm.osmfeature.types.attributes.AttributeDefinition;
import mil.nga.giat.geowave.cli.osm.osmfeature.types.attributes.AttributeType;
import mil.nga.giat.geowave.cli.osm.osmfeature.types.attributes.AttributeTypes;
import mil.nga.giat.geowave.core.index.StringUtils;

public class FeatureDefinitionSet
{
	public final static List<String> GeneralizedFeatures = Collections.unmodifiableList(new ArrayList<String>());
	public final static List<FeatureDefinition> Features = (new ArrayList<FeatureDefinition>());
	public final static Map<String, FeatureDataAdapter> featureAdapters = Collections
			.unmodifiableMap(new HashMap<String, FeatureDataAdapter>());
	public final static Map<String, SimpleFeatureType> featureTypes = Collections
			.unmodifiableMap(new HashMap<String, SimpleFeatureType>());
	private final static Object MUTEX = new Object();
	private static boolean initialized = false;
	private static final Logger LOGGER = LoggerFactory.getLogger(FeatureDefinitionSet.class);

	public static void initialize(
			String configFile ) {
		synchronized (MUTEX) {
			if (!initialized) {
				FeatureConfigParser fcp = new FeatureConfigParser();
				ByteArrayInputStream bais = new ByteArrayInputStream(
						configFile.getBytes(StringUtils.GEOWAVE_CHAR_SET));
				try {
					fcp.parseConfig(bais);
				}
				catch (IOException e) {
					LOGGER.error(
							"Unable to parse config file string",
							e);
				}
				finally {
					IOUtils.closeQuietly(bais);
				}

				for (FeatureDefinition fd : Features) {
					parseFeatureDefinition(fd);
				}

				initialized = true;
			}
		}
	}

	private static void parseFeatureDefinition(
			FeatureDefinition fd ) {
		final SimpleFeatureTypeBuilder sftb = new SimpleFeatureTypeBuilder();
		sftb.setName(fd.name);
		final AttributeTypeBuilder atb = new AttributeTypeBuilder();
		for (AttributeDefinition ad : fd.attributes) {
			AttributeType at = AttributeTypes.getAttributeType(ad.type);
			if (ad.name == null) {
				LOGGER.debug("yo"); // should this be deleted?
			}
			if (at != null) {
				sftb.add(atb.binding(
						at.getClassType()).nillable(
						true).buildDescriptor(
						normalizeOsmNames(ad.name)));
			}
		}
		SimpleFeatureType sft = sftb.buildFeatureType();
		featureTypes.put(
				fd.name,
				sft);
		featureAdapters.put(
				fd.name,
				new FeatureDataAdapter(
						sft));
	}

	public static String normalizeOsmNames(
			String name ) {
		if (name == null) return null;

		return name.trim().toLowerCase(
				Locale.ENGLISH).replace(
				":",
				"_");
	}

}
