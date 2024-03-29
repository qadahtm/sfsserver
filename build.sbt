import AssemblyKeys._

name := "SFSServer"

version := "0.1"

scalaVersion := "2.10.4"

packSettings

seq(
        packMain := Map(
        "WebServer" -> "edu.purdue.sfss.ui.Webserver"
        ,"SimpleClient" -> "edu.purdue.sfss.example.SimpleClient"    
	),
        // [Optional] (Generate .bat files for Windows. The default value is true)
        packGenerateWindowsBatFile := true,
        // [Optional] jar file name format in pack/lib folder (Since 0.5.0)
        //   "default"   (project name)-(version).jar 
        //   "full"      (organization name).(project name)-(version).jar
        //   "no-version" (organization name).(project name).jar
        //   "original"  (Preserve original jar file names)
        packJarNameConvention := "default",
        // [Optional] List full class paths in the launch scripts (default is false) (since 0.5.1)
        packExpandedClasspath := true
      ) 

resolvers ++= Seq(
  	"spray repo" at "http://repo.spray.io",
	"java maven" at "http://download.java.net/maven/2",
  	"Boundless Maven Repository" at "http://repo.boundlessgeo.com/main",
  	"Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/releases/",
  	"Secured Central Repository" at "https://repo1.maven.org/maven2"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka"  %% "akka-actor"       % "2.3.6",
  "com.typesafe.akka"  %% "akka-slf4j"       % "2.3.6",
  "com.typesafe.akka"  %% "akka-remote"       % "2.3.6",
  "io.spray"           %% "spray-can"        % "1.3.2",
  "io.spray"           %% "spray-routing"    % "1.3.2",
  "io.spray"           %% "spray-httpx"    % "1.3.2",
  "io.spray"           %% "spray-json"       % "1.3.0",
  "joda-time"		    % "joda-time" 		% "latest.integration",
  "org.joda" 			% "joda-convert" 	% "latest.integration",
  "log4j" % "log4j" % "1.2.14"
)


assemblySettings 

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Ywarn-dead-code",
  "-language:_",
  "-target:jvm-1.6",
  "-encoding", "UTF-8"
)
