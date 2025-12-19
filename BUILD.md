# Maven Build Configuration Guide

## Overview

This project uses **Apache Maven 3.9.11** with **Java 21** for build management. The build configuration supports multiple environments (dev, prod) and provides a Maven wrapper for consistent builds across different systems.

---

## Quick Start

### Prerequisites

- **Java 21** or higher installed
- **Maven 3.9.11+** (or use the included Maven wrapper)
- **PostgreSQL** database (for runtime)
- **Redis** server (for notifications)

### Using Maven Wrapper (Recommended)

The project includes Maven wrapper scripts that automatically download the correct Maven version:

```bash
# On Linux/Mac
./mvnw clean install

# On Windows
mvnw.cmd clean install
```

### Using System Maven

If you have Maven installed:

```bash
mvn clean install
```

---

## Build Commands

### Basic Build Operations

```bash
# Clean build artifacts
mvn clean

# Compile source code
mvn compile

# Run tests
mvn test

# Package as JAR
mvn package

# Install to local repository
mvn install

# Full clean build
mvn clean install
```

### Running the Application

```bash
# Using Spring Boot plugin
mvn spring-boot:run

# Using JavaFX plugin
mvn javafx:run

# Run packaged JAR
java -jar target/magictech-management-1.0-SNAPSHOT.jar
```

---

## Maven Profiles

The project includes three build profiles for different scenarios:

### 1. Development Profile (Default)

**Activation**: Active by default

```bash
# Explicitly activate dev profile
mvn clean install -Pdev
```

**Features**:
- Development mode enabled
- Tests are executed
- Debug information included
- Fast iteration

### 2. Production Profile

**Activation**: Use `-Pprod` flag

```bash
mvn clean install -Pprod
```

**Features**:
- Production mode
- Optimized compilation
- Tests are executed
- Debug symbols stripped
- Performance optimized

### 3. Fast Build Profile

**Activation**: Use `-Pfast` flag

```bash
mvn clean install -Pfast
```

**Features**:
- **Skips all tests** (for rapid iteration)
- Skips Javadoc generation
- Skips source packaging
- Fastest build time

**Use when**: You need quick builds during active development

---

## Build Options

### Skip Tests

```bash
# Skip tests during build
mvn clean install -DskipTests

# Or use fast profile
mvn clean install -Pfast
```

### Show Warnings and Deprecations

```bash
mvn clean install -Dshowwarnings=true -Dshowdeprecation=true
```

### Verbose Output

```bash
mvn clean install -X
```

### Offline Mode

```bash
mvn clean install -o
```

---

## Maven Build Plugins

### Configured Plugins

| Plugin | Version | Purpose |
|--------|---------|---------|
| maven-compiler-plugin | 3.11.0 | Java compilation with Java 21 |
| maven-resources-plugin | 3.3.1 | Resource filtering and copying |
| maven-surefire-plugin | 3.0.0 | Unit test execution |
| maven-jar-plugin | 3.3.0 | JAR packaging with manifest |
| spring-boot-maven-plugin | (inherited) | Spring Boot packaging & execution |
| javafx-maven-plugin | 0.0.8 | JavaFX runtime support |

### Plugin Features

#### Compiler Plugin
- **Source/Target**: Java 21
- **Encoding**: UTF-8
- **Warnings**: Enabled
- **Deprecation**: Enabled
- **Parameters**: Preserved for reflection

#### Spring Boot Plugin
- **Main Class**: `com.magictech.MainApp`
- **Layout**: JAR (executable)
- **Build Info**: Generated
- **Repackaging**: Enabled

#### JavaFX Plugin
- **Main Class**: `com.magictech.MainApp`
- **Launcher**: `magictech-launcher`
- **Optimization**: Enabled (compress level 2)

---

## Resource Filtering

The build configuration includes **resource filtering** for property files:

**Filtered Resources** (variables replaced):
- `**/*.properties`
- `**/*.yml`
- `**/*.yaml`

**Non-Filtered Resources** (copied as-is):
- FXML files
- CSS files
- Images
- All other resources

### Using Maven Properties in Resources

In `application.properties`:

```properties
# These will be replaced during build
application.version=@project.version@
application.name=@project.name@
build.timestamp=@maven.build.timestamp@
```

---

## Dependency Management

### Centralized Version Management

All dependency versions are managed in `<properties>`:

```xml
<properties>
    <!-- Core -->
    <java.version>21</java.version>
    <javafx.version>21.0.1</javafx.version>

    <!-- UI Libraries -->
    <atlantafx.version>2.0.1</atlantafx.version>
    <controlsfx.version>11.2.1</controlsfx.version>
    <ikonli.version>12.3.1</ikonli.version>

    <!-- Document Processing -->
    <pdfbox.version>2.0.29</pdfbox.version>
    <itext.version>7.2.5</itext.version>
    <poi.version>5.2.5</poi.version>

    <!-- Utilities -->
    <json.version>20231013</json.version>
    <commons.compress.version>1.26.0</commons.compress.version>
    <commons.collections4.version>4.4</commons.collections4.version>
</properties>
```

### Updating Dependencies

To update a dependency version:

1. Find the version property in `pom.xml`
2. Update the version number
3. Rebuild: `mvn clean install`

### Viewing Dependency Tree

```bash
# Show full dependency tree
mvn dependency:tree

# Save to file
mvn dependency:tree > dependencies.txt

# Show only direct dependencies
mvn dependency:list
```

---

## Common Build Scenarios

### Scenario 1: First Time Setup

```bash
# Clone repository
git clone <repository-url>
cd magictech-management

# Build and install dependencies
./mvnw clean install

# Run application
./mvnw spring-boot:run
```

### Scenario 2: Daily Development

```bash
# Quick build (skip tests)
./mvnw clean install -Pfast

# Run application
./mvnw spring-boot:run
```

### Scenario 3: Before Committing Code

```bash
# Full build with all tests
./mvnw clean install

# Check for dependency issues
./mvnw dependency:analyze
```

### Scenario 4: Production Build

```bash
# Clean production build
./mvnw clean install -Pprod

# Package is in: target/magictech-management-1.0-SNAPSHOT.jar
```

### Scenario 5: Update Dependencies

```bash
# Check for dependency updates
./mvnw versions:display-dependency-updates

# Check for plugin updates
./mvnw versions:display-plugin-updates
```

---

## Build Output

### Build Artifacts Location

```
target/
├── magictech-management-1.0-SNAPSHOT.jar    # Executable JAR
├── classes/                                  # Compiled classes
├── test-classes/                            # Test classes
├── maven-status/                            # Build metadata
└── maven-archiver/                          # JAR metadata
```

### Executable JAR

The Spring Boot plugin creates a **fat JAR** with all dependencies:

```bash
# Run the JAR
java -jar target/magictech-management-1.0-SNAPSHOT.jar

# The JAR includes:
# - Application classes
# - All dependencies
# - Resources (FXML, CSS, images)
# - Spring Boot launcher
```

---

## Troubleshooting

### Build Issues

#### Issue: "JAVA_HOME not set"

**Solution**:
```bash
# Linux/Mac
export JAVA_HOME=/path/to/java-21

# Windows
set JAVA_HOME=C:\path\to\java-21
```

#### Issue: "Maven dependencies not downloading"

**Solution**:
```bash
# Force update
./mvnw clean install -U

# Clear local repository cache
rm -rf ~/.m2/repository/
./mvnw clean install
```

#### Issue: "Tests failing"

**Solution**:
```bash
# Run tests in debug mode
./mvnw test -X

# Skip tests temporarily
./mvnw clean install -DskipTests
```

#### Issue: "JavaFX modules not found"

**Solution**:
```bash
# Ensure Java 21+ is being used
java -version

# Rebuild with fresh dependencies
./mvnw clean install -U
```

### Network Issues

If Maven cannot download dependencies due to network issues:

```bash
# Use a mirror (add to ~/.m2/settings.xml)
<mirrors>
    <mirror>
        <id>aliyun-maven</id>
        <mirrorOf>central</mirrorOf>
        <name>Aliyun Maven</name>
        <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
</mirrors>

# Or use offline mode with cached dependencies
./mvnw clean install -o
```

---

## IDE Integration

### IntelliJ IDEA

1. **Open Project**: `File > Open` → Select `pom.xml`
2. **Auto-Import**: Enable Maven auto-import
3. **JDK**: Set Project SDK to Java 21
4. **Run Configuration**:
   - Main Class: `com.magictech.MainApp`
   - VM Options: `--add-modules javafx.controls,javafx.fxml`

### Eclipse

1. **Import**: `File > Import > Existing Maven Projects`
2. **Select**: Choose project directory
3. **Maven Update**: Right-click project → `Maven > Update Project`

### VS Code

1. **Extensions**: Install "Java Extension Pack" and "Maven for Java"
2. **Open Folder**: Open project root
3. **Maven**: Use Maven sidebar for build commands

---

## Maven Wrapper Details

### What is Maven Wrapper?

Maven Wrapper ensures everyone on your team uses the **same Maven version** without requiring manual installation.

### Files

```
.mvn/
└── wrapper/
    ├── maven-wrapper.properties    # Wrapper configuration
    └── maven-wrapper.jar           # Wrapper implementation (downloaded on first run)
mvnw                                 # Unix script
mvnw.cmd                             # Windows script
```

### First Run

On first run, the wrapper will:
1. Download Maven 3.9.11
2. Cache it in `~/.m2/wrapper/`
3. Execute your build command

### Updating Wrapper

To update the Maven wrapper version:

```bash
mvn wrapper:wrapper -Dmaven=3.9.11
```

---

## Best Practices

### Development Workflow

1. **Always use Maven wrapper** for consistent builds
2. **Run full build before committing**: `./mvnw clean install`
3. **Use fast profile during active development**: `./mvnw install -Pfast`
4. **Keep dependencies up-to-date**: Check monthly for updates
5. **Don't commit `target/` directory**: Already in `.gitignore`

### Performance Tips

1. **Enable parallel builds**:
   ```bash
   ./mvnw clean install -T 4  # Use 4 threads
   ```

2. **Increase Maven memory**:
   ```bash
   export MAVEN_OPTS="-Xmx2g -XX:MaxPermSize=512m"
   ```

3. **Use offline mode when possible**:
   ```bash
   ./mvnw clean install -o
   ```

---

## Environment-Specific Configuration

### Development Environment

```bash
./mvnw spring-boot:run -Pdev
```

Application runs with:
- Dev profile active
- Debug enabled
- Local database
- Hot reload enabled

### Production Environment

```bash
./mvnw clean install -Pprod
java -jar target/magictech-management-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

Application runs with:
- Production profile active
- Optimized performance
- Production database
- Security enabled

---

## Additional Resources

### Documentation Links

- [Maven Official Documentation](https://maven.apache.org/guides/)
- [Spring Boot Maven Plugin](https://docs.spring.io/spring-boot/docs/current/maven-plugin/reference/htmlsingle/)
- [JavaFX Maven Plugin](https://github.com/openjfx/javafx-maven-plugin)

### Project-Specific

- **CLAUDE.md**: AI assistant guide with architecture details
- **README.md**: Project overview and setup
- **pom.xml**: Complete Maven configuration

---

## Support

For build issues or questions:

1. Check this documentation
2. Review `CLAUDE.md` for architecture details
3. Check Maven logs: `./mvnw clean install -X`
4. Check [Maven Central](https://search.maven.org/) for dependency issues

---

**Last Updated**: December 2024
**Maven Version**: 3.9.11
**Java Version**: 21
