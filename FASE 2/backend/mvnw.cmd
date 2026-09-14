@ECHO OFF
SETLOCAL
SET "MAVEN_VERSION=3.9.11"
SET "MAVEN_ARCHIVE=apache-maven-%MAVEN_VERSION%-bin.zip"
SET "MAVEN_URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/%MAVEN_ARCHIVE%"
SET "MAVEN_SHA512=03e2d65d4483a3396980629f260e25cac0d8b6f7f2791e4dc20bc83f9514db8d0f05b0479e699a5f34679250c49c8e52e961262ded468a20de0be254d8207076"
IF DEFINED MAVEN_USER_HOME (SET "CACHE_ROOT=%MAVEN_USER_HOME%\wrapper\dists\apache-maven-%MAVEN_VERSION%") ELSE (SET "CACHE_ROOT=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%")
SET "MAVEN_HOME_DIR=%CACHE_ROOT%\apache-maven-%MAVEN_VERSION%"
IF NOT EXIST "%MAVEN_HOME_DIR%\bin\mvn.cmd" (
  IF NOT EXIST "%CACHE_ROOT%" MKDIR "%CACHE_ROOT%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$p='%CACHE_ROOT%\%MAVEN_ARCHIVE%'; Invoke-WebRequest -UseBasicParsing '%MAVEN_URL%' -OutFile $p; if ((Get-FileHash $p -Algorithm SHA512).Hash.ToLower() -ne '%MAVEN_SHA512%') { throw 'Checksum de Maven inválido' }; Expand-Archive -Force $p '%CACHE_ROOT%'; Remove-Item $p"
  IF ERRORLEVEL 1 EXIT /B 1
)
CALL "%MAVEN_HOME_DIR%\bin\mvn.cmd" %*
ENDLOCAL
