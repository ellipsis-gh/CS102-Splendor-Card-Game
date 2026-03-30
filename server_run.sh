javac -d classes -cp src src/interfaces/*.java src/model/*.java src/network/ServerMain.java
java -cp classes network.ServerMain
