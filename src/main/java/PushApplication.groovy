#!/usr/bin/env groovy
package main.java
import main.java.urbancode.CommandHelper
import groovy.util.logging.Slf4j

@Slf4j
class InternalPushApplication
{
	
	def deployAppToBlueMix(props)
	{
		def workDir = new File('.').canonicalFile;
		def commandHelper = new CommandHelper(workDir);
		try
		{
			setupPath(props,commandHelper)
			login(props, commandHelper)
			pushApplication(props, commandHelper)
			logout(props, commandHelper)
		}catch(e)
		{
			log.error("Error:", e)
			//System.exit(1)
			throw e
		}
	}
	
	static private def setupPath(props, commandHelper)
	{
		try {
			def curPath = System.getenv("PATH")
			def pluginHome = new File(System.getenv("PLUGIN_HOME"))
			//log.info("Setup of path using plugin home: " + pluginHome)
			def binDir = new File(pluginHome, "bin")
			def newPath = curPath+":"+binDir.absolutePath
			commandHelper.addEnvironmentVariable("PATH", newPath)
			def cfHome = new File(props['PLUGIN_INPUT_PROPS']).parentFile
			//log.info("Setting CF_HOME to: " + cfHome)
			commandHelper.addEnvironmentVariable("CF_HOME", cfHome.toString())
		} catch(e){
			log.error("ERROR setting path: ${e.message}")
			throw e
		}
	}
	
	static private def login(props, commandHelper)
	{
		// Set cf api
		try {
			def commandArgs = [props.pathToCF, "api", props.api]
			if (props.selfSigned) {
				commandArgs << "--skip-ssl-validation"
			}
			commandHelper.runCommand("Setting BlueMix target api", commandArgs)
		} catch(e){
			log.error("ERROR setting api: ${e.message}")
			throw e
		}
		
		// Authenticate with user and password
		try {
			def commandArgs = [props.pathToCF, "auth", props.user, props.password];
			commandHelper.runCommand("Authenticating with Bluemix", commandArgs);
		} catch(e){
			log.error("ERROR authenticating : ${e.message}")
			throw e
		}
		
		// Set target org
		try {
			def commandArgs = [props.pathToCF, "target", "-o", props.org];
			commandHelper.runCommand("Setting BlueMix target organization", commandArgs);
		} catch(e){
			log.error("ERROR setting target organization : ${e.message}")
			throw e
		}
		
		// Ensure space exists. create-space does nothing if space
		// exists
		try {
			def commandArgs = [props.pathToCF, "create-space", props.space];
			commandHelper.runCommand("Creating BlueMix space", commandArgs);
		} catch(e){
			log.error("ERROR creating space : ${e.message}")
			throw e
		}
		
		// Set target space
		try {
			def commandArgs = [props.pathToCF, "target", "-s", props.space];
			commandHelper.runCommand("Setting BlueMix target space", commandArgs);
		} catch(e){
			log.error("ERROR setting target space : ${e.message}")
			throw e
		}
	}
	
	static private def pushApplication(props, commandHelper)
	{
		// Push the application
		try {
			def commandArgs = [props.pathToCF, "push", props.appName];
		
			if (props.buildpack) {
				commandArgs << "-b";
				commandArgs << props.buildpack;
			}
		
			if (props.manifest) {
				commandArgs << "-f";
				commandArgs << props.manifest;
			}
		
			if (props.instances) {
				commandArgs << "-i";
				commandArgs << props.instances;
			}
		
			if (props.memory) {
				commandArgs << "-m";
				commandArgs << props.memory;
			}
			
			if (props.disk) {
				commandArgs << "-k";
				commandArgs << props.disk;
			}
		
			if (props.path) {
				commandArgs << "-p" ;
				commandArgs << props.path;
			}
		
			if (props.domain) {
				commandArgs << "-d" ;
				commandArgs << props.domain;
			}
		
			if (props.subdomain) {
				commandArgs << "-n" ;
				commandArgs << props.subdomain;
			}
			
			if (props.stack) {
				commandArgs << "-s";
				commandArgs << props.stack;
			}
			
			if (props.timeout) {
				commandArgs << "-t";
				commandArgs << props.timeout;
			}
		
			if (props.nostart == "true") {
				commandArgs << "--no-start";
			}
		
			if (props.noroute == "true") {
				commandArgs << "--no-route";
			}
		
			if (props.nomanifest == "true") {
				commandArgs << "--no-manifest";
			}
		
			if (props.nohostname == "true") {
				commandArgs << "--no-hostname";
			}
			
			if (props.randomroute == "true") {
				commandArgs << "--random-route";
			}
		
			commandHelper.runCommand("Deploying BlueMix application", commandArgs);
		} catch(e){
			log.error("ERROR authenticating : ${e.message}")
			throw e
		}
	}

	static private def logout(props, commandHelper)
	{
		try {
			def commandArgs = [props.pathToCF, "logout"];
			commandHelper.runCommand("Logout from BlueMix system", commandArgs);
		} catch(e){
			log.error("ERROR logging out : ${e.message}")
			throw e
		}
	}	
}

final def props = new Properties();
final def inputPropsFile = new File(args[0]);
try {
	inputPropsStream = new FileInputStream(inputPropsFile);
	props.load(inputPropsStream);
}
catch (IOException e) {
	throw e;
}
try{
	def internal = new InternalPushApplication()
	internal.deployAppToBlueMix(props)
	//System.exit(0);
	return
}
catch(e)
{
	throw e
	//System.exit(1);
}