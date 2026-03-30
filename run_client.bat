@echo off
javac -d classes -sourcepath src src\network\ClientMain.java
java -cp classes network.ClientMain
pause
