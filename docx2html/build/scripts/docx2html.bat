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

set CLASSPATH=%APP_HOME%\lib\docx2html.jar;%APP_HOME%\lib\fr.opensagres.poi.xwpf.converter.xhtml-2.0.4.jar;%APP_HOME%\lib\fr.opensagres.poi.xwpf.converter.core-2.0.4.jar;%APP_HOME%\lib\poi-ooxml-5.2.5.jar;%APP_HOME%\lib\flexmark-all-0.64.8.jar;%APP_HOME%\lib\flexmark-html2md-converter-0.64.8.jar;%APP_HOME%\lib\flexmark-pdf-converter-0.64.8.jar;%APP_HOME%\lib\jsoup-1.17.2.jar;%APP_HOME%\lib\slf4j-simple-2.0.12.jar;%APP_HOME%\lib\slf4j-api-2.0.12.jar;%APP_HOME%\lib\flexmark-profile-pegdown-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-emoji-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-aside-0.64.8.jar;%APP_HOME%\lib\flexmark-jira-converter-0.64.8.jar;%APP_HOME%\lib\flexmark-youtrack-converter-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-tables-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-abbreviation-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-admonition-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-anchorlink-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-enumerated-reference-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-attributes-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-autolink-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-definition-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-escaped-character-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-footnotes-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-gfm-issues-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-gfm-strikethrough-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-gfm-tasklist-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-gfm-users-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-macros-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-gitlab-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-jekyll-front-matter-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-jekyll-tag-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-media-tags-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-resizable-image-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-ins-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-xwiki-macros-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-superscript-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-toc-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-typographic-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-wikilink-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-yaml-front-matter-0.64.8.jar;%APP_HOME%\lib\flexmark-ext-youtube-embedded-0.64.8.jar;%APP_HOME%\lib\flexmark-0.64.8.jar;%APP_HOME%\lib\poi-5.2.5.jar;%APP_HOME%\lib\poi-ooxml-lite-5.2.5.jar;%APP_HOME%\lib\poi-ooxml-full-5.2.0.jar;%APP_HOME%\lib\xmlbeans-5.2.0.jar;%APP_HOME%\lib\commons-compress-1.25.0.jar;%APP_HOME%\lib\commons-io-2.15.0.jar;%APP_HOME%\lib\curvesapi-1.08.jar;%APP_HOME%\lib\log4j-core-2.17.1.jar;%APP_HOME%\lib\log4j-api-2.21.1.jar;%APP_HOME%\lib\commons-collections4-4.4.jar;%APP_HOME%\lib\fr.opensagres.xdocreport.core-2.0.4.jar;%APP_HOME%\lib\flexmark-util-0.64.8.jar;%APP_HOME%\lib\flexmark-util-format-0.64.8.jar;%APP_HOME%\lib\flexmark-util-ast-0.64.8.jar;%APP_HOME%\lib\flexmark-util-builder-0.64.8.jar;%APP_HOME%\lib\flexmark-util-dependency-0.64.8.jar;%APP_HOME%\lib\flexmark-util-html-0.64.8.jar;%APP_HOME%\lib\flexmark-util-options-0.64.8.jar;%APP_HOME%\lib\flexmark-util-sequence-0.64.8.jar;%APP_HOME%\lib\flexmark-util-collection-0.64.8.jar;%APP_HOME%\lib\flexmark-util-data-0.64.8.jar;%APP_HOME%\lib\flexmark-util-misc-0.64.8.jar;%APP_HOME%\lib\flexmark-util-visitor-0.64.8.jar;%APP_HOME%\lib\commons-codec-1.16.0.jar;%APP_HOME%\lib\commons-math3-3.6.1.jar;%APP_HOME%\lib\SparseBitSet-1.3.jar;%APP_HOME%\lib\annotations-24.0.1.jar;%APP_HOME%\lib\autolink-0.6.0.jar;%APP_HOME%\lib\icu4j-72.1.jar;%APP_HOME%\lib\openhtmltopdf-pdfbox-1.0.10.jar;%APP_HOME%\lib\openhtmltopdf-rtl-support-1.0.10.jar;%APP_HOME%\lib\openhtmltopdf-core-1.0.10.jar;%APP_HOME%\lib\graphics2d-0.32.jar;%APP_HOME%\lib\pdfbox-2.0.24.jar;%APP_HOME%\lib\xmpbox-2.0.24.jar;%APP_HOME%\lib\fontbox-2.0.24.jar;%APP_HOME%\lib\commons-logging-1.2.jar

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
