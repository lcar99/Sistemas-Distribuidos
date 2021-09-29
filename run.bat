@ECHO OFF 

ECHO Compiling client and server

javac servidor.java
javac cliente.java

ECHO Executing server and clients

::Para todos os 4 comandos a seguir e necessario o path absoluto
start "" "C:\Users\Lucas\Desktop\SD\09-29\servidor.bat"
start "" "C:\Users\Lucas\Desktop\SD\09-29\cliente.bat"
start "" "C:\Users\Lucas\Desktop\SD\09-29\cliente.bat"
start "" "C:\Users\Lucas\Desktop\SD\09-29\cliente.bat"

EXIT