# vscode-scalajs-aurora

This Project is a port of the [helloworld-minimal-sample] to [ScalaJS]. It is based on the extension [accessible-scala].

Please check there on how to describe a VSCode Extension.

#Important Note
I could not get scalatest to work after multiple attempts.  Since this is front end work, get testing done on dependencies

## Your first Extension
A step-by-step tutorial for **ScalaJS** using this example project.

Here is the original: [visualstudio.com/api/get-started](https://code.visualstudio.com/api/get-started/your-first-extension)

### Setup


* Open VSCode in the `sbt` console:

      open

  This will run `fastOptJS`and then open the Extension Host of _VSCode_.

* Run the Hello World command from the Command Palette (`⇧⌘P`) in the new VSCode window.
* Type `hello` and select `Hello Aurora`.
* Enter any text in the edit box in that location
  * You should see a Notification _Hello World _yourtext_

## Debug your Extension

There seems not to be support for debugging Scala code directly in _VSCode_ (at least I did not find how to do this).

However you can achieve this:

1. Run the extension from VSCode. There is a launch configuration in `.vscode/launch.json`. so just press `F5` and another _VS Code_ window opens in debug mode.
2. You have to set the Breakpoints in the generated Javascript (`out/extension.js`). Through `extension.js.map` the breakpoint will stop in your Scala code.

> If you work in this mode, make sure in your _sbt console_ to transpile to Javascript continuously (`~fastOptJS`). So you see all your changes.

 ## How-To / Further Information
Check out the (vsc-extension-quickstart.md) for some general _VSCode Extension_ explanations.

The project uses the following:
* **ScalaJS** for general coding: [ScalaJS]

  The `extension.js` from [helloworld-minimal-sample], is now `src/main/scala/extension.scala`.

* **ScalablyTyped** for JavaScript facades: [Scalably Typed]

  The Hello World only uses the `vscode` bindings. But you can use any other _Typescript_ binding supported by _ScalablyTyped_.

* **sbt** for building the project: [SBT]
* **scalajs-bundler** for bundling the JavaScript dependencies: [scalajs-bundler].

## Running the Patient Tracker (Available in the `patient-tracker` branch)

* Launch the extension and execute the **Patient Tracker** command from the Command Palette (`⇧⌘P`) in the newly opened VSCode window.

### Important Prerequisites:

 Ensure that your Docker database is running and the [patient-server] is up and operational.

This project also depends on the `dataimportcsv3s` repository. Ensure that it is published locally on your system.

Clone the repository from GitHub:
[https://github.com/yash-a-18/dataimportcsv3s.git](https://github.com/yash-a-18/dataimportcsv3s.git)

## Quick Start Guide: Running the Repository

Follow these steps to set up and run the project:

1. Install Node.js dependencies:
  ```bash
  npm install
  ```

2. Navigate to the `axiompatienttracker` directory:
  ```bash
  cd axiompatienttracker
  ```

3. Install additional dependencies:
  ```bash
  npm install
  ```

4. Return to the root directory:
  ```bash
  cd ..
  ```

5. Start the Scala build tool (sbt):
  ```bash
  sbt
  ```

6. Choose one of the following options to run the project:

  **Option 1: Open the Extension Host**
  ```bash
  open
  ```

  **Option 2: Continuously transpile to JavaScript and debug**
  ```bash
  ~fastOptJS
  ```
  Then press `F5` in VSCode to launch the extension in debug mode.

By following these steps, you can quickly set up and execute the repository.

## Open Points

* Publishing to the Marketplace.
* Add Testing.

[accessible-scala]: https://marketplace.visualstudio.com/items?itemName=scala-center.accessible-scala
[helloworld-minimal-sample]: https://github.com/Microsoft/vscode-extension-samples/tree/master/helloworld-minimal-sample
[Scalably Typed]: https://github.com/oyvindberg/ScalablyTyped
[SBT]: https://www.scala-sbt.org
[ScalaJS]: http://www.scala-js.org
[scalajs-bundler]: https://github.com/scalacenter/scalajs-bundler
[patient-server]: https://github.com/yash-a-18/patient-server.git
