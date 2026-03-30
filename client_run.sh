javac -d classes -cp src src/interfaces/*.java src/model/*.java src/network/ClientMain.java
java -cp classes network.ClientMain
