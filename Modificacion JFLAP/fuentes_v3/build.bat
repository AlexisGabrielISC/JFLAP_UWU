@echo off
REM Requiere JDK 21+ en el PATH y JFLAP_UWU.jar (tu JAR original renombrado) en esta carpeta.
if not exist JFLAP_UWU.jar (
  echo Falta JFLAP_UWU.jar en esta carpeta
  exit /b 1
)
if exist out rmdir /s /q out
mkdir out
javac -nowarn -encoding UTF-8 -cp JFLAP_UWU.jar -d out src\plus\JFLAPPlus.java src\gui\sim\ControlPanel.java src\gui\sim\ConfigurationController.java src\automata\Note.java src\automata\State.java src\automata\Automaton.java src\automata\turing\TuringMachine.java src\gui\viewer\StateDrawer.java src\gui\editor\AutomatonSizeSlider.java src\gui\editor\ArrowTool.java src\file\xml\AutomatonTransducer.java
if errorlevel 1 exit /b 1
copy /y JFLAP_UWU.jar JFLAP_v3.jar >nul
jar uf JFLAP_v3.jar -C out .
echo Listo: JFLAP_v3.jar
