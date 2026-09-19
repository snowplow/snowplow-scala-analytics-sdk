addSbtPlugin("com.typesafe"       % "sbt-mima-plugin" % "1.1.1")
addSbtPlugin("org.scoverage"      % "sbt-scoverage"   % "2.4.4")
addSbtPlugin("com.github.sbt"     % "sbt-site"        % "1.7.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"         % "0.4.0")
addSbtPlugin("com.github.sbt"     % "sbt-ci-release"  % "1.9.0")
addSbtPlugin("org.scalameta"      % "sbt-scalafmt"    % "2.4.6")

libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
