@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=-Xmx64m -Xms64m

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
@rem Get command-line arguments, handling Windowz variants

if not "%OS%" == "Windows_NT" goto win9xME_args

@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=

@rem Get Windows command line arguments with correct case preservation
set GET_ARGS=%*
set _CMD_ARGS=

:get_arg
if "%GET_ARGS%"=="" goto :done_get_args
set _CMD_ARGS=%_CMD_ARGS% %GET_ARGS%
shift
goto :get_arg

:done_get_args
set CMD_LINE_ARGS=%_CMD_ARGS%

@rem Shift the remaining args to skip the first two
shift
shift

@rem Execute gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% -classpath "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" endlocal

:fail
set VARIABLE_NAME=GRADLE_EXIT_CONSOLE_ERROR_
if not "%VARIABLE_NAME%"=="1" goto :end
set VARIABLE_NAME=
echo.
echo ERROR: Gradle could not be started.
goto :eof
