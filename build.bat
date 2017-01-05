@echo Building...
@echo off
del bin\*.* /Q
dir /s /B src\*.groovy > tmpsources.txt
call groovyc -cp .\lib\* -d bin @tmpsources.txt
copy store.jks bin
del tmpsources.txt
@echo Done!