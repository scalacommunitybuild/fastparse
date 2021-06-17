ThisBuild / organization := "org.scalameta"
ThisBuild / name := "fastparse-v2"
ThisBuild / scalaVersion := "2.13.6"
ThisBuild / testFrameworks += new TestFramework("utest.runner.Framework")

val commonSettings = Seq(
  libraryDependencies += "com.lihaoyi" %% "utest" % "0.7.4" % Test,
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
)

publish / skip := true  // don't publish root project

lazy val fastparse = project.in(file("fastparse"))
  .settings(commonSettings)
  .settings(
    // Compile
    libraryDependencies += "com.lihaoyi" %% "sourcecode" % "0.2.1",
    libraryDependencies += "com.lihaoyi" %% "geny" % "0.6.0",
    unmanagedSourceDirectories in Compile ++= Seq(baseDirectory.value / "src", baseDirectory.value / "src-jvm"),
    // Test
    unmanagedSourceDirectories in Test += baseDirectory.value / "test" / "src",
    unmanagedResourceDirectories in Test += baseDirectory.value / "test" / "resources",
    // source generation
    sourceGenerators in Compile += Def.task {
      val dir = (sourceManaged in Compile).value
      val file = dir/"fastparse"/"SequencerGen.scala"
      // Only go up to 21, because adding the last element makes it 22
      val tuples = (2 to 21).map{ i =>
        val ts = (1 to i) map ("T" + _)
        val chunks = (1 to i) map { n =>
          s"t._$n"
        }
        val tsD = (ts :+ "D").mkString(",")
        val anys = ts.map(_ => "Any").mkString(", ")
        s"""
            val BaseSequencer$i: Sequencer[($anys), Any, ($anys, Any)] =
              Sequencer0((t, d) => (${chunks.mkString(", ")}, d))
            implicit def Sequencer$i[$tsD]: Sequencer[(${ts.mkString(", ")}), D, ($tsD)] =
              BaseSequencer$i.asInstanceOf[Sequencer[(${ts.mkString(", ")}), D, ($tsD)]]
            """
      }
      val output = s"""
        package fastparse
        trait SequencerGen[Sequencer[_, _, _]] extends LowestPriSequencer[Sequencer]{
          protected[this] def Sequencer0[A, B, C](f: (A, B) => C): Sequencer[A, B, C]
          ${tuples.mkString("\n")}
        }
        trait LowestPriSequencer[Sequencer[_, _, _]]{
          protected[this] def Sequencer0[A, B, C](f: (A, B) => C): Sequencer[A, B, C]
          implicit def Sequencer1[T1, T2]: Sequencer[T1, T2, (T1, T2)] = Sequencer0{case (t1, t2) => (t1, t2)}
        }
      """.stripMargin
      IO.write(file, output)
      Seq(file)
    }
  )

lazy val scalaparse = project.in(file("scalaparse"))
  .settings(commonSettings)
  .settings(
    Compile / unmanagedSourceDirectories += baseDirectory.value / "src",
    Test / unmanagedSourceDirectories += baseDirectory.value / "test" / "src",
    Test / unmanagedSourceDirectories += baseDirectory.value / "test" / "src-jvm",
    Test / unmanagedResourceDirectories += baseDirectory.value / "test" / "resources",
    libraryDependencies += "junit" % "junit" % "4.13" % Test,
    libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  )
  .dependsOn(fastparse)
