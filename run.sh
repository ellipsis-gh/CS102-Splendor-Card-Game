javac -d classes -cp "src:lib/jansi-2.4.1.jar" src/Main.java
java -Dfile.encoding=UTF-8 -cp "classes:lib/jansi-2.4.1.jar" Main
