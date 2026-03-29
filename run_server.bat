@echo off
javac -d classes -cp src src\network\ServerMain.java
java -cp classes network.ServerMain
pause
