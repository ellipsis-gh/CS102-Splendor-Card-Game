@echo off
javac -d classes -sourcepath src src\network\ServerMain.java
java -cp classes network.ServerMain
pause
