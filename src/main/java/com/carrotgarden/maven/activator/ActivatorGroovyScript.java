package com.carrotgarden.maven.activator;

import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.apache.maven.model.profile.activation.PropertyProfileActivator;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

/**
 * Activate project profile based on Groovy property expression.
 */
@Component( //
		role = ActivatorAny.class, //
		// Note: keep hint unique.
		hint = Activator.GROOVYSCRIPT //
)
public class ActivatorGroovyScript extends PropertyProfileActivator implements ActivatorAny {

	@Override
	public String activatorName() {
		return Activator.GROOVYSCRIPT;
	}

	@Override
	public String activatorEngine() {
		// See: org.codehaus.groovy.jsr223.GroovyScriptEngineFactory
		return "groovy";
	}

	@Requirement
	protected Logger logger;

	@Override
	public Logger logger() {
		return logger;
	}

	@Override
	public boolean isActive( //
			Profile profile, //
			ProfileActivationContext context, //
			ModelProblemCollector problems //
	) {
		return hasActivationAction(profile, context, problems);
	}

	@Override
	public boolean presentInConfig( //
			Profile profile, //
			ProfileActivationContext context, //
			ModelProblemCollector problems //
	) {
		return hasActivationProperty(profile, context, problems);
	}

}
