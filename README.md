# SimpleRickAI
```
MyApp/
├── build.gradle                                    # Project-level Gradle build file
├── settings.gradle                                 # Gradle settings file
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties               # Defines the Gradle version used
├── app/
│   ├── build.gradle                                # App-module Gradle file (dependencies)
│   └── src/
│       └── main/
│           ├── java/
│ _app.jsx  |  └── com/
│           │       └── simplerick/
│           │           ├── MainActivity.kt         # Entry point for permission and service control
│           │           └── FloatingAssistantService.kt # Core service for floating chat and API calls
│           ├── res/
│           │   ├── drawable/
│           │   │   ├── badge_online.xml          # Background for the floating bubble
│           │   │   ├── ic_launcher.xml           # App icon foreground (Placeholder)
│           │   │   └── ic_launcher_background.xml  # App icon background (Placeholder)
│           │   ├── layout/
│           │   │   ├── activity_main.xml         # Layout for the main permission activity
│           │   │   └── layout_floating_widget.xml  # Layout for the floating chat window and bubble
│           │   └── values/
│           │       ├── colors.xml                # Color definitions
│           │       ├── strings.xml               # Text strings
│           │       └── styles.xml                # Theme and style definitions
│           └── AndroidManifest.xml               # Application permissions and component declarations
└── build/                                          # Output directory (created by Gradle)
```


``` engine/
  core/
    security/
      gateway.py
      analyzer.py
      risk_scoring.py
      self_learning.py
      attacks/
        prompt_injection.py
        jailbreaks.py
        poisoning.py
      defenses/
        sanitizers.py
        anomaly_detector.py
        policy_rules.py
  infra/
    logging/
      security_logger.py
    storage/
      security_db.py
config/
  security.yaml
```
