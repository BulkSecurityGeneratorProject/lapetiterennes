package fr.lpr.membership;

import com.google.common.base.Joiner;
import fr.lpr.membership.config.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;

@SpringBootApplication
@EnableJpaAuditing
@Slf4j
public class Application {

	@Inject
	private Environment env;

	/**
	 * <p>
	 * Initializes membership.
	 * </p>
	 * <p>
	 * Spring profiles can be configured with a program arguments --spring.profiles.active=your-active-profile
	 * </p>
	 * <p>
	 * You can find more information on how profiles work with JHipster on <a
	 * href="http://jhipster.github.io/profiles.html">http://jhipster.github.io/profiles.html</a>.
	 * </p>
	 */
	@PostConstruct
	public void initApplication() {
		if (env.getActiveProfiles().length == 0) {
			log.warn("No Spring profile configured, running with default configuration");
		} else {
			log.info("Running with Spring profile(s) : {}", Arrays.toString(env.getActiveProfiles()));
			final Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
			if (activeProfiles.contains("dev") && activeProfiles.contains("prod")) {
				log.error("You have misconfigured your application! " + "It should not run with both the 'dev' and 'prod' profiles at the same time.");
			}
			if (activeProfiles.contains("prod") && activeProfiles.contains("fast")) {
				log.error("You have misconfigured your application! " + "It should not run with both the 'prod' and 'fast' profiles at the same time.");
			}
			if (activeProfiles.contains("dev") && activeProfiles.contains("cloud")) {
				log.error("You have misconfigured your application! " + "It should not run with both the 'dev' and 'cloud' profiles at the same time.");
			}
		}
	}

	/**
	 * Main method, used to run the application.
	 *
	 * @param args
	 *            program arguments
	 * @throws UnknownHostException
	 *             if host if unknown
	 */
	public static void main(String[] args) throws UnknownHostException {
		final SpringApplication app = new SpringApplication(Application.class);
		final SimpleCommandLinePropertySource source = new SimpleCommandLinePropertySource(args);
		addDefaultProfile(app, source);
		addLiquibaseScanPackages();
		final Environment env = app.run(args).getEnvironment();
		log.info("Access URLs:\n----------------------------------------------------------\n\t" + "Local: \t\thttp://127.0.0.1:{}\n\t"
				+ "External: \thttp://{}:{}\n----------------------------------------------------------", env.getProperty("server.port"), InetAddress
				.getLocalHost().getHostAddress(), env.getProperty("server.port"));

	}

	/**
	 * If no profile has been configured, set by default the "dev" profile.
	 */
	private static void addDefaultProfile(SpringApplication app, SimpleCommandLinePropertySource source) {
		if (!source.containsProperty("spring.profiles.active") && !System.getenv().containsKey("SPRING_PROFILES_ACTIVE")) {

			app.setAdditionalProfiles(Constants.SPRING_PROFILE_DEVELOPMENT);
		}
	}

	/**
	 * Set the liquibases.scan.packages to avoid an exception from ServiceLocator.
	 */
	private static void addLiquibaseScanPackages() {
		System.setProperty(
				"liquibase.scan.packages",
				Joiner.on(",").join("liquibase.change", "liquibase.database", "liquibase.parser", "liquibase.precondition", "liquibase.datatype",
						"liquibase.serializer", "liquibase.sqlgenerator", "liquibase.executor", "liquibase.snapshot", "liquibase.logging", "liquibase.diff",
						"liquibase.structure", "liquibase.structurecompare", "liquibase.lockservice", "liquibase.ext", "liquibase.changelog"));
	}
}
