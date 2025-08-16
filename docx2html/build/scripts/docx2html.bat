@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  docx2html startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Add default JVM options here. You can also use JAVA_OPTS and DOCX2HTML_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windows variants

if not "%OS%" == "Windows_NT" goto win9xME_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\docx2html.jar;%APP_HOME%\lib\fr.opensagres.poi.xwpf.converter.xhtml-2.0.4.jar;%APP_HOME%\lib\fr.opensagres.poi.xwpf.converter.core-2.0.4.jar;%APP_HOME%\lib\poi-ooxml-5.2.5.jar;%APP_HOME%\lib\jsoup-1.17.2.jar;%APP_HOME%\lib\slf4j-simple-2.0.12.jar;%APP_HOME%\lib\slf4j-api-2.0.12.jar;%APP_HOME%\lib\poi-5.2.5.jar;%APP_HOME%\lib\poi-ooxml-lite-5.2.5.jar;%APP_HOME%\lib\poi-ooxml-full-5.2.0.jar;%APP_HOME%\lib\xmlbeans-5.2.0.jar;%APP_HOME%\lib\commons-compress-1.25.0.jar;%APP_HOME%\lib\commons-io-2.15.0.jar;%APP_HOME%\lib\curvesapi-1.08.jar;%APP_HOME%\lib\log4j-core-2.17.1.jar;%APP_HOME%\lib\log4j-api-2.21.1.jar;%APP_HOME%\lib\commons-collections4-4.4.jar;%APP_HOME%\lib\fr.opensagres.xdocreport.core-2.0.4.jar;%APP_HOME%\lib\commons-codec-1.16.0.jar;%APP_HOME%\lib\commons-math3-3.6.1.jar;%APP_HOME%\lib\SparseBitSet-1.3.jar

@rem Execute docx2html
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %DOCX2HTML_OPTS%  -classpath "%CLASSPATH%" com.example.DocxToHtmlApp %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable DOCX2HTML_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%DOCX2HTML_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
