:: Updated bat files to resolve typing issues in the command line

:: Compile the client program
call ant -f build_datetimeclient.xml compile jar

:: compile the server program
call ant -f build_datetimeserver.xml compile jar

:: Run the server program, pass port as an argument.
java -jar build/jar/DateTimeServer.jar 7775

pause;