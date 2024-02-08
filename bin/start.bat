@echo off
call mvn-clean-install.bat
if %errorlevel% equ 0 (
	cd bin
	call open-webpage.bat
    java -jar ../target/path-0.0.1-SNAPSHOT.jar
) else (
    echo Build failed.
)
pause