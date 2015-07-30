# fta
Fast Transfer Agent 

# What is fta?
fta is a simple and Lightweight file transfer agent written by java,By using the zero-copy, in case of the absence of cpu overhead, can reach hundreds of megabytes per second throughput.fta  based on C/S structure, The client is FtaClient, server is FtaServer.

# Usage
1.start server by run startServer.sh
2.call FtaClient in your java program, as following:
FtaClient client = new FtaClient(host, port);
long sendSize = client.copyAndCreateFile(srcFile, dstcFile);

