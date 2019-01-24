ALL: server client

server: Server.cpp
	g++ -std=c++0x Server.cpp -o server -g -lrt

client: Client.cpp
	g++ -std=c++0x Client.cpp -o client -g -lrt

clean:
	rm -f client
	rm -f server