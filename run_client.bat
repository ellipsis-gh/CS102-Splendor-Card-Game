@echo off
javac -d classes -cp src src\network\ClientMain.java
java -cp classes network.ClientMain
pause
