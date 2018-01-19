package com.carrotgarden.maven.activator;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblem.Version;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.profile.ProfileActivationContext;

/**
 * Static activator functions.
 */
public interface SupportFunction {

	/**
	 * Provide script execution context variables.
	 */
	static Map<String, Object> bindingsFrom(ProfileActivationContext context, Model project) {
		// Note: keep order.
		Map<String, Object> bindings = new HashMap<>();
		// Inject project props: override defaults
		bindings.putAll(context.getProjectProperties());
		bindings.putAll(bindingsFrom(project.getProperties()));
		// Inject system props, override previous.
		bindings.putAll(context.getSystemProperties());
		// Inject user props, override previous.
		bindings.putAll(context.getUserProperties());
		// Expose default variable context.
		bindings.put("value", bindings);
		// Expose resolved pom.xml model.
		bindings.put("project", project);
		return bindings;
	}

	/**
	 * Provide script execution context variables.
	 */
	static Map<String, Object> bindingsFrom(Properties properties) {
		Map<String, Object> bindings = new HashMap<>();
		properties.forEach((key, value) -> bindings.put(key.toString(), value.toString()));
		return bindings;
	}

	/**
	 * Default model resolution request.
	 */
	static ModelBuildingRequest buildRequest( //
			ProfileActivationContext context //
	) {
		ModelBuildingRequest request = new DefaultModelBuildingRequest();
		request.setPomFile(projectPOM(context));
		request.setSystemProperties(convertFrom(context.getSystemProperties()));
		request.setUserProperties(convertFrom(context.getUserProperties()));
		request.setLocationTracking(false);
		request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
		return request;
	}

	/**
	 * Change properties format.
	 */
	static Properties convertFrom(Map<String, String> map) {
		Properties props = new Properties();
		props.putAll(map);
		return props;
	}

	/**
	 * Discover script manager from Plexus class path.
	 */
	static ScriptEngineManager engineManager() {
		return new ScriptEngineManager(SupportFunction.class.getClassLoader());
	}

	/**
	 * Detect if profile has a property based activation.
	 */
	static boolean hasAnyProperty(Profile profile) {
		return true && //
				profile != null && //
				profile.getActivation() != null && //
				profile.getActivation().getProperty() != null && //
				true;
	}

	/**
	 * Detect if profile has a property with name.
	 */
	static boolean hasPropertyName(Profile profile) {
		return true && //
				hasAnyProperty(profile) && //
				profile.getActivation().getProperty().getName() != null && //
				true;
	}

	/**
	 * Detect if profile has a property with value.
	 */
	static boolean hasPropertyValue(Profile profile) {
		return true && //
				hasAnyProperty(profile) && //
				profile.getActivation().getProperty().getValue() != null && //
				true;
	}

	/**
	 * Produce default problem descriptor.
	 */
	static ModelProblemCollectorRequest problemRequest() {
		return new ModelProblemCollectorRequest(Severity.ERROR, Version.BASE);
	}

	/**
	 * Extract null-safe profile identity.
	 */
	static String profileId(Profile profile) {
		return profile.getId() == null ? "" : profile.getId();
	}

	/**
	 * Extract optional project name from context.
	 */
	static String projectName(ProfileActivationContext context) {
		String missing = "<missing>";
		File basedir = context.getProjectDirectory();
		if (basedir == null) {
			return missing;
		}
		File pomFile = new File(basedir, "pom.xml");
		if (pomFile.exists()) {
			Model model = readMavenModel(pomFile);
			String artifactId = model.getArtifactId();
			if (artifactId != null) {
				return artifactId;
			} else {
				return missing;
			}
		} else {
			return basedir.getName();
		}
	}

	/**
	 * Extract optional project pom.xml file from context.
	 */
	static File projectPOM(ProfileActivationContext context) {
		File basedir = context.getProjectDirectory();
		if (basedir == null) {
			return null;
		}
		File pomFile = new File(basedir, "pom.xml");
		if (pomFile.exists()) {
			return pomFile.getAbsoluteFile();
		} else {
			return null;
		}
	}

	/**
	 * Extract profile property name.
	 */
	static String propertyName(Profile profile) {
		return profile.getActivation().getProperty().getName();
	}

	/**
	 * Extract profile property value.
	 */
	static String propertyValue(Profile profile) {
		return profile.getActivation().getProperty().getValue();
	}

	/**
	 * Fail-safe pom.xml model reader.
	 */
	static Model readMavenModel(File pomFile) {
		try {
			MavenXpp3Reader pomReader = new MavenXpp3Reader();
			FileInputStream fileInput = new FileInputStream(pomFile);
			InputStreamReader fileReader = new InputStreamReader(fileInput, "UTF-8");
			return pomReader.read(fileReader);
		} catch (Throwable e) {
			return new Model();
		}
	}

	/**
	 * Inject new problem in reporter.
	 */
	static void registerProblem( //
			String message, //
			Exception error, //
			Profile profile, //
			ModelProblemCollector problems //
	) {
		ModelProblemCollectorRequest request = problemRequest() //
				.setMessage(message) //
				.setException(error) //
				.setLocation(profile.getLocation("")) //
		;
		problems.add(request);
	}

	/**
	 * Render engine bindings report.
	 */
	static String renderBindings(Map<String, Object> map) {
		Map<String, Object> sortedMap = new TreeMap<>(map);
		StringBuilder text = new StringBuilder();
		for (Entry<String, Object> entry : sortedMap.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			text.append("   ");
			text.append(key);
			text.append(" : ");
			text.append(value.getClass().getSimpleName());
			text.append(" = ");
			text.append(value.toString());
			text.append("\n");
		}
		return text.toString();
	}

	/**
	 * Render script engine report.
	 */
	static void renderEngine(ScriptEngineFactory factory, StringBuilder text) {
		text.append("Script Engine:\n");
		String engineName = factory.getEngineName();
		String engineVersion = factory.getEngineVersion();
		String languageName = factory.getLanguageName();
		String languageVersion = factory.getLanguageVersion();
		List<String> nameList = factory.getNames();
		List<String> mimetypeList = factory.getMimeTypes();
		List<String> extensionList = factory.getExtensions();
		text.append(String.format("   Engine: %s (%s) \n", engineName, engineVersion));
		text.append(String.format("   Language: %s (%s) \n", languageName, languageVersion));
		text.append(String.format("   Name List: %s \n", renderStringList(nameList)));
		text.append(String.format("   MimeType List: %s \n", renderStringList(mimetypeList)));
		text.append(String.format("   Extension List: %s \n", renderStringList(extensionList)));
	}

	/**
	 * Report available script engines.
	 */
	static String renderEngineList() {
		StringBuilder text = new StringBuilder();
		ScriptEngineManager manager = engineManager();
		List<ScriptEngineFactory> factoryList = manager.getEngineFactories();
		for (ScriptEngineFactory factory : factoryList) {
			renderEngine(factory, text);
		}
		return text.toString();
	}

	static String renderModel(Model model) {
		StringBuilder text = new StringBuilder();
		return text.toString();
	}

	/**
	 * Report list as flat string.
	 */
	static String renderStringList(List<String> list) {
		return Arrays.toString(list.toArray());
	}

	/**
	 * Discover script engine by some name.
	 */
	static ScriptEngine scriptEngine(String pattern) {
		ScriptEngineManager manager = engineManager();
		ScriptEngine engine = manager.getEngineByName(pattern);
		if (engine != null) {
			return engine;
		}
		engine = manager.getEngineByExtension(pattern);
		if (engine != null) {
			return engine;
		}
		engine = manager.getEngineByMimeType(pattern);
		if (engine != null) {
			return engine;
		}
		// Engine is missing.
		return null;
	}

}
