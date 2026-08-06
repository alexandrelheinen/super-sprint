plugins {
	java
	application
}

group = "supelec.supersprint"
version = providers.gradleProperty("appVersion")
	.orElse(providers.environmentVariable("APP_VERSION"))
	.orElse("0.0.0-dev")
	.get()

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
}

fun bashExecutable(): String {
	val osName = System.getProperty("os.name").lowercase()
	if (!osName.contains("windows")) {
		return "bash"
	}
	val candidates = listOfNotNull(
		System.getenv("ProgramFiles")?.let { "$it\\Git\\bin\\bash.exe" },
		System.getenv("ProgramFiles(x86)")?.let { "$it\\Git\\bin\\bash.exe" },
		"C:\\Program Files\\Git\\bin\\bash.exe"
	)
	return candidates.firstOrNull { file(it).exists() } ?: "bash"
}

val bash = bashExecutable()

repositories {
	mavenCentral()
}

dependencies {
	testImplementation(platform("org.junit:junit-bom:5.10.2"))
	testImplementation("org.junit.jupiter:junit-jupiter")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
	mainClass.set("controller.Main")
}

tasks.test {
	useJUnitPlatform()
	jvmArgs("-Djava.awt.headless=true")
}

val generatedResourcesDir = layout.buildDirectory.dir("generated/resources/main")

sourceSets {
	main {
		resources {
			// Generated sprites/config override hand-authored resources on duplicates.
			srcDir(generatedResourcesDir)
		}
	}
}

tasks.register<Exec>("prepareCarSprites") {
	group = "build"
	description = "Slice cars.png into race/menu sprites and write cars.properties"
	val outputDir = generatedResourcesDir
	inputs.file("src/main/resources/sprites/cars.png")
	inputs.file("scripts/prepare-car-sprites.sh")
	outputs.dir(outputDir.map { it.dir("sprites") })
	outputs.file(outputDir.map { it.file("data/config/cars.properties") })
	commandLine(bash, "scripts/prepare-car-sprites.sh", outputDir.get().asFile.absolutePath)
}

tasks.register<Exec>("prepareKenneySprites") {
	group = "build"
	description = "Extract Kenney scenery sprites used at runtime"
	val outputDir = generatedResourcesDir
	inputs.file("third_party/kenney-top-down-tanks-redux/kenney_topdownTanksRedux.zip")
	inputs.file("scripts/prepare-kenney-sprites.sh")
	outputs.dir(outputDir.map { it.dir("sprites/kenney") })
	commandLine(bash, "scripts/prepare-kenney-sprites.sh", outputDir.get().asFile.absolutePath)
}

tasks.register<JavaExec>("generateTrackPreviews") {
	group = "build"
	description = "Render track preview PNGs into generated resources"
	dependsOn(tasks.compileJava, "prepareCarSprites", "prepareKenneySprites")
	// Avoid sourceSets.main.runtimeClasspath here: it depends on `classes`,
	// which depends on processResources, which would cycle through this task.
	classpath = files(
		tasks.compileJava.get().destinationDirectory,
		"src/main/resources",
		generatedResourcesDir
	)
	mainClass.set("view.TrackPreviewGenerator")
	val spritesOut = generatedResourcesDir.map { it.dir("sprites").asFile }
	outputs.files(
		(0 until 4).map { index ->
			generatedResourcesDir.map { it.file("sprites/track_preview_%02d.png".format(index)) }
		}
	)
	args(spritesOut.get().absolutePath)
	jvmArgs("-Djava.awt.headless=true")
}

tasks.named<ProcessResources>("processResources") {
	dependsOn("prepareCarSprites", "prepareKenneySprites", "generateTrackPreviews")
	duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.named<Jar>("jar") {
	manifest {
		attributes["Main-Class"] = "controller.Main"
	}
	dependsOn(tasks.processResources)
}

tasks.register<Exec>("smokeTest") {
	group = "verification"
	description = "Headless launch smoke test (Xvfb + short timeout)"
	dependsOn(tasks.installDist)
	commandLine(bash, "scripts/smoke-test.sh")
}

tasks.register<Exec>("packageRelease") {
	group = "distribution"
	description = "Build portable zip; pass -PappImage=true for jpackage app-image"
	dependsOn(tasks.jar)
	val appVersion = project.version.toString()
	val doAppImage = providers.gradleProperty("appImage").orElse("false")
	environment("PACKAGE_JAR", tasks.jar.get().archiveFile.get().asFile.absolutePath)
	commandLine(
		bash,
		"scripts/package-release.sh",
		"--version", appVersion,
		*(if (doAppImage.get() == "true") arrayOf("--app-image") else emptyArray())
	)
}

tasks.register<Exec>("recordDemo") {
	group = "distribution"
	description = "Record a demo race MP4. Pass -PTRACK= -PCARS= [-PLAPS=] [-PDEMO_MP4=]"
	dependsOn(tasks.jar)
	val track = providers.gradleProperty("TRACK")
	val cars = providers.gradleProperty("CARS")
	val laps = providers.gradleProperty("LAPS").orElse("3")
	val output = providers.gradleProperty("DEMO_MP4")
	val jarPath = tasks.jar.flatMap { it.archiveFile }
	// Placeholder replaced in doFirst once -PTRACK/-PCARS are available.
	commandLine("bash", "-c", "echo 'recordDemo misconfigured'; exit 1")
	doFirst {
		if (!track.isPresent || !cars.isPresent) {
			throw GradleException("Missing -PTRACK=<trackId> and/or -PCARS=<carIds>")
		}
		environment("RECORD_JAR", jarPath.get().asFile.absolutePath)
		val args = mutableListOf(bash, "scripts/record-demo-race.sh")
		if (output.isPresent) {
			args += output.get()
		}
		args += listOf(track.get(), cars.get(), laps.get())
		commandLine(args)
	}
}

tasks.register<JavaExec>("demoRace") {
	group = "application"
	description = "All-AI exhibition race. Pass -PTRACK=3 -PCARS=0,0,0,0 [-PLAPS=3]"
	dependsOn(tasks.classes, tasks.processResources)
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set("view.DemoRaceCapture")
	val track = providers.gradleProperty("TRACK")
	val cars = providers.gradleProperty("CARS")
	val laps = providers.gradleProperty("LAPS").orElse("3")
	doFirst {
		if (!track.isPresent || !cars.isPresent) {
			throw GradleException("Missing -PTRACK=<trackId> and/or -PCARS=<carIds>")
		}
		args(track.get(), cars.get(), laps.get())
	}
}
