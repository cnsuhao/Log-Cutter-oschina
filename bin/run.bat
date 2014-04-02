@ECHO OFF

echo.

if "%JRE_HOME%" == "" goto :use_jdk
	set _JAVA_HOME="%JRE_HOME%"
	echo Using  JRE_HOME : %_JAVA_HOME%
	goto :set_classpath
	
:use_jdk
set _JAVA_HOME="%JAVA_HOME%"
echo Using JAVA_HOME : %_JAVA_HOME%

:set_classpath
set _CLASSPATH="%CLASSPATH%"
echo Using CLASSPATH : %_CLASSPATH%

set _JAVA=%_JAVA_HOME%\bin\java

set APP_PATH="%~dp0.."
set APP_CLASSPATH=%APP_PATH%\classes
set APP_LIBPATH=%APP_PATH%\lib
set APP_MAIN_CLASS=org.jessma.logcutter.main.LogCutter

@ECHO ON

%_JAVA% -Duser.dir=%APP_PATH% -Djava.ext.dirs=%APP_LIBPATH% -cp %APP_CLASSPATH%;%_CLASSPATH% %APP_MAIN_CLASS% %*
