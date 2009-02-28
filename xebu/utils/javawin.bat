
rem Copyright 2006 Helsinki Institute for Information Technology

rem This file is a part of Fuego middleware.  Fuego middleware is free
rem software; you can redistribute it and/or modify it under the terms
rem of the MIT license, included as the file MIT-LICENSE in the Fuego
rem middleware source distribution.  If you did not receive the MIT
rem license with the distribution, write to the Fuego Core project at
rem fuego-core-users@hoslab.cs.helsinki.fi.

echo off
echo a Java Virtual Machine wrapper script for Fuego Core
echo Usage: arguments to JVM
echo This wrapper script is intended to eliminate any need to set
echo classpaths by defining the extension directories suitably.  
echo The variable FUEGOCORE_CODE should be set to point to the
echo Fuego Core root directory; if the variable is not set, current
echo directory is assumed.  Do not set the variable FUEGOCORE_HOME, since
echo that is used internally by the Fuego Core project.

if not "%FUEGOCORE_CODE%"=="" goto _jumper
  set FUEGOCORE_CODE=.

:_jumper

java -Djava.ext.dirs=%FUEGOCORE_CODE%\build\lib;%FUEGOCORE_CODE%\contrib\jar %1 %2 %3 %4 %5 %6 %7
