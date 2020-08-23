@ECHO OFF
IF NOT "%SPECCHIO_HOME%"=="" GOTO start
SET SPECCHIO_HOME=%{INSTALL_PATH}

:start
start /d "%SPECCHIO_HOME%" /min java -jar -Dncsa.hdf.hdf5lib.H5.hdf5lib=%SPECCHIO_HOME%\win32\jhdf5.dll -Djdk.security.allowNonCaAnchor=true specchio-client.jar
