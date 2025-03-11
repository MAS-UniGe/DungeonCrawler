   #!/bin/bash

   # Path to the JavaFX SDK library (update to your actual path)
   JAVA_FX_LIB="libs/javafx-sdk-17.0.14/lib"

   # Path to the JAR file (ensure it points to the correct location)
   JAR_FILE="out/artifacts/Project_jar/Project.jar"

   # Check if JavaFX lib directory exists
   if [ ! -d "$JAVA_FX_LIB" ]; then
       echo "Error: JavaFX library directory '$JAVA_FX_LIB' does not exist."
       exit 1
   fi

   # Check if the JAR file exists
   if [ ! -f "$JAR_FILE" ]; then
       echo "Error: JAR file '$JAR_FILE' not found."
       exit 1
   fi

   # Run the Java command
   java --module-path "$JAVA_FX_LIB" --add-modules javafx.controls,javafx.fxml -jar "$JAR_FILE"