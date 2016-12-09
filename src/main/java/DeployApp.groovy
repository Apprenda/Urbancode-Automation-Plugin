package main.java
import main.java.urbancode.AirPluginTool
import groovy.util.logging.Slf4j

@Slf4j
class InternalDeployApp {
	def detectVersion(versionInfo, props)
	{
		log.info("Starting version detection.")
		def newVersionRequired = false
		def targetVersion = versionInfo.currentVersion.alias
		def newVerStage = versionInfo.currentVersion.stage
		log.info("targetVersion: " + targetVersion)
		log.info("newVerStage: " + newVerStage)
		if(versionInfo.currentVersion.stage == 'Definition' || versionInfo.currentVersion.stage == 'Sandbox'){
			log.info("Current version is in definition/sandbox, so we will patch v1")
			return [newVersionRequired:false, targetVersion:'v1', newVerStage:versionInfo.currentVersion.stage]
		}
		else if(versionInfo.currentVersion.stage == 'Published')
		{
			log.info("Current version is in published, so we need to find the latest version.")
			def oldVerNo = versionInfo.currentVersion.alias.substring(1).toInteger()
			def newVerNo = oldVerNo			
			def versions = ApprendaClient.GetVersionInfo(props)
			versions.getData().each { version ->
				def verNo = version.alias.substring(1).toInteger()
				if (newVerNo < verNo) {
					newVerNo = verNo
					newVerStage = version.stage
					if(newVerStage != 'Published')
					{
						newVersionRequired = false
					}
				}
				log.info("Old Version #: " + oldVerNo)
				log.info("New Version #: " + newVerNo)
				log.info("Target Stage: " + newVerStage)
			} 
			if(newVerNo == oldVerNo && (newVerStage == 'Published'))
			{
				newVersionRequired = true
				newVerNo++
			}
			targetVersion = "v" + newVerNo.toString()
			return [newVersionRequired:newVersionRequired, targetVersion:targetVersion, newVerStage:newVerStage]
		}
		else
		{
			log.error("We received data we didn't expect. Exiting...")
			throw new IllegalArgumentException('We received malformed data: ' + versionInfo.toString)
		}
	}
	
	public static debugFromMain(data)
	{
		log.info(data.toString())
	}
	
	public static logFromMain(exception)
	{
		log.error("We hit an issue.", exception)
	}
}

	final def apTool = new AirPluginTool(this.args[0], this.args[1])
	props = apTool.getStepProperties()
	try {
		internal = new InternalDeployApp()
		def getApps = ApprendaClient.GetApplicationInfo(props)
		// inject here that if we don't have a new application, we need to create it.
		
		if(getApps == null || getApps.getData().currentVersion == null)
		{
			ApprendaClient.NewApplication(props)
			// should come back now as v1 and definition
			getApps = ApprendaClient.GetApplicationInfo(props)
			InternalDeployApp.debugFromMain(getApps.getData().toString())
		}
		InternalDeployApp.debugFromMain(getApps.getData())
		def versionOutput = internal.detectVersion(getApps.getData(), props)
		if(versionOutput.newVersionRequired) 
		{
			ApprendaClient.PostNewVersion(props, versionOutput.targetVersion)
		}
		else
		if(getApps.currentVersion.stage == 'Sandbox' || versionOutput.newVerStage == 'Sandbox')
		{
			ApprendaClient.Demote(props, versionOutput.targetVersion)	
		}
		ApprendaClient.PatchApplication(props, versionOutput.targetVersion)
		if(props.Stage == 'Sandbox' || props.Stage == 'Published')
		{
			ApprendaClient.Promote(props, versionOutput.targetVersion)
		}
	}
	catch (e) {
		// Sadly, slf4j doesn't like to log from a main closure in groovy... so we trick it.
		InternalDeployApp.logFromMain(e)
		//System.exit 1
		throw e
	}
	//System.exit 0
	return