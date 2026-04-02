@echo off
javac -encoding UTF-8 -d classes -cp "src;lib\jansi-2.4.1.jar" src\network\ServerMain.java
java -Dfile.encoding=UTF-8 -cp "classes;lib\jansi-2.4.1.jar" network.ServerMain
pause
