# Scala.js Facades for React Components

Scala.js Facades is a multi-module SBT project providing Scala.js facades for React components. It consists of a core SimpleFacade library and facades for various React component libraries.

Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

## Working Effectively

### Prerequisites and Installation
- Install SBT 1.11.7:
  ```bash
  curl -L "https://github.com/sbt/sbt/releases/download/v1.11.7/sbt-1.11.7.tgz" | tar xz -C /tmp/
  export PATH="/tmp/sbt/bin:$PATH"
  ```
- Ensure Java 8+ is available (Java 17 works well)
- Ensure Node.js 20+ and npm are available
- Install yarn globally: `npm install -g yarn`

### Build and Test Commands
- Bootstrap the project: `sbt about` -- takes ~8 seconds
- List all projects: `sbt projects`
- Compile all working projects: 
  ```bash
  sbt "; project simpleFacade; compile; project reactSelect; compile; project reactInputMask; compile; project reactAutocomplete; compile; project reactWidgets; compile; project reactWaypoint; compile; project reactDatepicker; compile; project reactPhoneNumberInput; compile"
  ```
  Takes ~1-2 minutes total. NEVER CANCEL.
- Test all working projects:
  ```bash
  sbt "; project simpleFacade; test; project reactSelect; test; project reactInputMask; test; project reactAutocomplete; test; project reactWidgets; test; project reactWaypoint; test; project reactDatepicker; test; project reactPhoneNumberInput; test"
  ```
  Takes ~3-5 minutes total. NEVER CANCEL. Set timeout to 10+ minutes.

### Individual Project Commands
- Compile specific project: `sbt "project <projectName>" compile` -- takes 8-20 seconds
- Test specific project: `sbt "project <projectName>" test` -- takes 20-60 seconds
- Available working projects: simpleFacade, reactSelect, reactInputMask, reactAutocomplete, reactWidgets, reactWaypoint, reactDatepicker, reactPhoneNumberInput

### Known Issues and Limitations
- Material-UI projects (materialUiCore, materialUiBase, materialUiLab) require react-docgen setup and currently fail to build
- Do NOT attempt to build Material-UI projects without first setting up react-docgen properly
- The build system automatically downloads npm dependencies using yarn, which may show deprecation warnings (these are safe to ignore)
- Peer dependency warnings for React packages are expected and safe to ignore

## Validation

### Testing Your Changes
- Always test your changes by running the relevant project's test suite
- For core SimpleFacade changes: `sbt "project simpleFacade" test` -- takes ~60 seconds
- For React component facade changes: `sbt "project <reactComponent>" test` -- takes 20-45 seconds
- No specific linting commands are configured, but the project uses .scalafmt.conf for formatting
- Always verify that existing tests continue to pass after making changes

### Manual Validation Scenarios
After making changes, validate with these scenarios:
1. **SimpleFacade changes**: Run all dependent React component tests
   ```bash
   sbt "; project reactSelect; test; project reactInputMask; test"
   ```
2. **React component facade changes**: Test the specific component and SimpleFacade
   ```bash
   sbt "; project simpleFacade; test; project <changedComponent>; test"
   ```
3. **Build configuration changes**: Test cross-compilation
   ```bash
   sbt "++2.13.18" "project simpleFacade" compile
   ```

## Project Structure

### Repository Layout
```
scalajs-facades/
├── .github/workflows/          # CI/CD configuration
├── project/                    # SBT build configuration
├── simpleFacade/              # Core SimpleFacade library
├── reactSelect/               # React Select component facade
├── reactInputMask/            # React Input Mask component facade
├── reactAutocomplete/         # React Autocomplete component facade
├── reactWidgets/              # React Widgets component facade
├── reactWaypoint/             # React Waypoint component facade
├── reactDatepicker/           # React Datepicker component facade
├── reactPhoneNumberInput/     # React Phone Number Input component facade
├── materialUiCore/            # Material-UI Core (requires react-docgen)
├── materialUiBase/            # Material-UI Base (requires react-docgen)
├── materialUiLab/             # Material-UI Lab (requires react-docgen)
├── build.sbt                  # Main SBT build file
├── ci.sbt                     # CI configuration
└── .scalafmt.conf            # Scala formatting configuration
```

### Key Projects
1. **simpleFacade** - Core library providing the SimpleFacade functionality for creating React component facades
2. **React Component Facades** - Individual facades for popular React components (8 projects)
3. **Material-UI Facades** - Auto-generated facades for Material-UI components (3 projects, currently not building)

## Common Tasks

### Working with SimpleFacade (Core Library)
- Location: `/simpleFacade/src/main/scala/io/github/nafg/simplefacade/`
- Test: `sbt "project simpleFacade" test` -- takes ~1 minute
- This is the foundational library that other facades depend on

### Working with React Component Facades
- Each facade project follows the same pattern: `/react<Component>/src/main/scala/`
- All React facade projects use yarn for npm dependency management
- Testing any React facade: `sbt "project react<Component>" test` -- takes 20-45 seconds

### Build Timing Expectations
- SBT startup: ~8 seconds
- Individual project compile: 8-20 seconds
- Individual project test: 20-60 seconds (includes npm dependency download)
- Full working project test suite: 3-5 minutes
- **NEVER CANCEL builds or tests** - they include npm dependency downloads which can be slow

### Cross-compilation
- Projects support both Scala 2.13.16 and Scala 3.3.7
- Default is Scala 3.3.7
- Use `sbt "++2.13.16" "project <projectName>" <command>` to test with Scala 2.13
- Cross-compilation takes slightly longer (~17 seconds vs 8 seconds for compile)

## Frequently Referenced Commands

### List all projects
```bash
sbt projects
```
Output:
```
In file:/home/runner/work/scalajs-facades/scalajs-facades/
   materialUiBase
   materialUiCore
   materialUiLab
   reactAutocomplete
   reactDatepicker
   reactInputMask
   reactPhoneNumberInput
   reactSelect
   reactWaypoint
   reactWidgets
 * scalajs-facades
   simpleFacade
```

### Show project information
```bash
sbt about
```

### Compile and test workflow for development
1. Make changes to relevant project
2. Compile: `sbt "project <projectName>" compile`
3. Test: `sbt "project <projectName>" test`
4. If working on SimpleFacade, also test dependent projects

### Emergency: Reset build state
If builds are failing mysteriously:
1. `rm -rf target */target`
2. `sbt clean`
3. `sbt "project <projectName>" compile`

## Complete Development Workflow Example

Example: Making a change to SimpleFacade and validating it works
```bash
# 1. Bootstrap and verify current state
sbt about                                    # ~8 seconds
sbt projects                                 # Verify projects list

# 2. Make your changes to SimpleFacade source code
# Edit files in simpleFacade/src/main/scala/...

# 3. Test your changes
sbt "project simpleFacade" compile           # ~8 seconds  
sbt "project simpleFacade" test              # ~60 seconds

# 4. Validate dependent projects still work
sbt "project reactSelect" test               # ~20-45 seconds
sbt "project reactInputMask" test            # ~20-45 seconds

# 5. Test cross-compilation if needed
sbt "++2.13.18" "project simpleFacade" compile  # ~17 seconds

# Total time: ~3-4 minutes for full validation
```