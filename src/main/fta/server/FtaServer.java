/**
 * @author jimmyquan
 * Fast file transfer agent
 */
package com.tencent.code.fta.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


public class FtaServer {
	
	private ServerSocketChannel serverSocketChannel = null;
	private Selector selector = null;
	
    private ThreadPoolExecutor threadPool = new ThreadPoolExecutor(8, 48, 5,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
            new ThreadPoolExecutor.DiscardOldestPolicy());
	
    private static final Logger logger = Logger.getLogger(FtaServer.class);
    
	public void startup() {
		PropertyConfigurator.configureAndWatch("conf/log4j.properties", 30000);
		Configuration.loadConf();
		logger.info("#########################################");
		logger.info("start fta server");
		try {
			serverSocketChannel = ServerSocketChannel.open();
			//阻塞模式
			serverSocketChannel.configureBlocking(true);
			//端口重用
			serverSocketChannel.socket().setReuseAddress(true);
			serverSocketChannel.socket().bind(new InetSocketAddress(10019));
			logger.info("start fta server success!");
			logger.info("#########################################");
			while (true) {
				SocketChannel conn =  serverSocketChannel.accept();
				logger.info("accept a new connection:"+ conn.toString()+Thread.currentThread());
				dispatch(conn);
			} 
		}catch (Exception e) {
				// TODO: handle exception
				logger.error("", e);
			}
	}
	
    private void dispatch(SocketChannel sc) throws IOException {
			sc.configureBlocking(true);
            Runnable handlerRunnable = new RecieveFileHandler(selector, sc);
            threadPool.execute(handlerRunnable);
            logger.info("current thred pool status, submitted:"+ threadPool.getTaskCount() + "complted:"+threadPool.getCompletedTaskCount());
	}
    
	
	public static void main(String[] args) {
		FtaServer fServer = new FtaServer();
		fServer.startup();
	}

}
