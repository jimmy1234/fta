/**
 * 
 */
package com.tencent.code.fta.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;


import org.apache.log4j.Logger;

import com.tencent.code.fta.MessageHeader;

public class RecieveFileHandler implements Runnable{

	private SocketChannel socketChannel = null;
	private MessageHeader messageHeader = null;
	private int waitTimes = 0;
	
	private static final Logger logger = Logger.getLogger(RecieveFileHandler.class);
	
	public RecieveFileHandler(Selector selector, SocketChannel socket) throws IOException {
		this.socketChannel = socket;
	}
	
	public void run()
	{
		try {
			read();
		} catch (Exception e) {
			try {
				socketChannel.close();
				socketChannel.socket().close();
			} catch (Exception e2) {
				e.printStackTrace();
			}
			e.printStackTrace();
		}
	}
	
	public void read() throws IOException
	{
		//消息头buffer,为了效率固定269字节
		ByteBuffer inputBuffer = ByteBuffer.allocate(269);
		socketChannel.read(inputBuffer);
		inputBuffer.flip();
		
		while (inputBuffer.remaining() > 0) {
			logger.info("inputBuffer remaining:"+inputBuffer.remaining());
			if(messageHeader == null) {
				if(inputBuffer.remaining() >= 14) {
					byte[] bMagic = new byte[4];
					inputBuffer.get(bMagic, 0, 4);
					logger.info("magic:"+MessageHeader.bytes2bigint(bMagic));
					if(!MessageHeader.isValidMessage(bMagic)) {
						break;
					}
					byte opTypeAndFileType = inputBuffer.get();
					byte opType = (byte) (opTypeAndFileType & 0xf0);
					byte fileType = (byte) (opTypeAndFileType & 0x0f);
					
					byte bFileLen = inputBuffer.get();
					//byte是有符号的,所以需要转成int,以表示0~255
					int iFileLen = bFileLen & 0xff;
					
					byte[] bBodyLen = new byte[8];
			        inputBuffer.get(bBodyLen, 0, 8);
			        long iBodyLen = MessageHeader.bytes2biglong(bBodyLen);
					messageHeader = new MessageHeader(iFileLen, opType, fileType, iBodyLen);
				} else {
					if(waitTimes == 3) {
						logger.warn("wait 3 times to get a full header, but failed, terminated the connection!");
						socketChannel.close();
						socketChannel.socket().close();
						break;
					}
					try {
						Thread.sleep(100);
					} catch (Exception e) {
						// TODO: handle exception
						logger.error("", e);
					}
					waitTimes ++;
				}
			} else if(messageHeader.getFileName() == null) {
				int iFileLen = messageHeader.getFileNameLen();
				if(inputBuffer.remaining() >=  255) {
				    byte[] bFileNmae = new byte[iFileLen]; 
			        inputBuffer.get(bFileNmae, 0, iFileLen);
			        String fileName = new String(bFileNmae);
			        messageHeader.setFileName(fileName);
			        inputBuffer.position(269);
			     }
			}
		}
		inputBuffer.clear();
		
		if(messageHeader.getFileType() == MessageHeader.FILE_TYPE_DIRECTORY && messageHeader.getOpType() == MessageHeader.OPERATION_TYPE_CREATE) {
			File directory = new File(messageHeader.getFileName());
			directory.mkdirs();
		}
		else if(messageHeader.getFileType() == MessageHeader.FILE_TYPE_FILE && messageHeader.getOpType() == MessageHeader.OPERATION_TYPE_CREATE) {
			File file = new File(messageHeader.getFileName());
			if(!file.exists()) {
			    writeToFile(file);
			} else {
				logger.warn("file:"+messageHeader.getFileName() +" has exists");
				socketChannel.close();
				socketChannel.socket().close();
			}
		}
		else if(messageHeader.getFileType() == MessageHeader.FILE_TYPE_FILE && messageHeader.getOpType() == MessageHeader.OPERATION_TYPE_OVERWRITING) {
			File file = new File(messageHeader.getFileName());
			 writeToFile(file);
		}
		else if (messageHeader.getOpType() == MessageHeader.OPERATION_TYPE_DELETE) {
			
				File file = new File(messageHeader.getFileName());
				if (!file.delete()) {
					logger.info("delete file:"+messageHeader.getFileName());
					file.delete();
				}
			
		}
	}

	private void writeToFile(File file) throws IOException {
		file.createNewFile();
		logger.info("start write file:"+ messageHeader.getFileName() +" body length:"+messageHeader.getBodyLen());
		FileOutputStream fos = null;
		FileChannel fcout = null;
		try {
			fos = new FileOutputStream(file);
			fcout = fos.getChannel();
			long transferBytes = fcout.transferFrom(socketChannel, 0, messageHeader.getBodyLen());
			if(transferBytes != messageHeader.getBodyLen()) {
				logger.error("file:"+messageHeader.getFileName() +" transfer error. Require:"+messageHeader.getBodyLen() + " bytes, in fact "+ transferBytes + " bytes");
			}
			fcout.force(false);
			logger.info("end write file:"+messageHeader.getFileName());

		}catch(IOException ex) {
			throw ex;
		}finally {
			fcout.close();
			fos.close();
			socketChannel.close();
			socketChannel.socket().close();
			
		}
	}
	
	
	private boolean allowDelete(String fileName) {
		if (fileName.contains(Configuration.getAllowDeleteDirectory())) {
			return true;
		}
		return false;
	}
}
