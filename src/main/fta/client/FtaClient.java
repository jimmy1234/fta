/**
 * client for transfer file
 */
package com.tencent.code.fta.client;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

import com.tencent.code.fta.MessageHeader;


public class FtaClient {

	private final static int MAGIC = 753195100;
	private SocketChannel client;
		
	public FtaClient(String serverIP, int port) {
		try {
			init(serverIP, port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void init(String serverIP, int port) throws IOException {
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(true);
		socketChannel.connect(new InetSocketAddress(serverIP, port));
		client = socketChannel;
	}
	
	
	public long  copyAndCreateFile(String srcFileName, String dstFileName) {
		try {
		    return sendFile(srcFileName, dstFileName, MessageHeader.OPERATION_TYPE_CREATE);
		}catch(IOException ex) {
			ex.printStackTrace();
		}
		return -1;
	}
	
	public long  copyAndOverridFile(String srcFileName, String dstFileName) {
		try {
		    return sendFile(srcFileName, dstFileName, MessageHeader.OPERATION_TYPE_OVERWRITING);
		}catch(IOException ex) {
			ex.printStackTrace();
		}
		return -1;
	}
	
	public long  deleteFile(String srcFileName, String dstFileName) {
		try {
		    return sendFile(srcFileName, dstFileName, MessageHeader.OPERATION_TYPE_DELETE);
		}catch(IOException ex) {
			ex.printStackTrace();
		}
		return -1;
	}
	
	/*public long[] nonAtomicCopyDirectory(String directory) {
		File file = new File(directory);
		if(file.isDirectory()) {
			try {
			    sendFile(directory, directory, MessageHeader.OPERATION_TYPE_CREATE);
			}catch(IOException ex) {
				ex.printStackTrace();
			}
		} else throw new IllegalArgumentException(directory + "is not directory");
	}*/

	/**
	 * send file to server
	 * @param srcFileName 源文件绝对路径(含路径)
	 * @param dstFileName 目标文件绝对路径(含路径)
	 * @param opType 0,1,2 创建，删除，覆盖
	 * @throws IOException
	 */
	private long sendFile(String srcFileName, String dstFileName, int opType) throws IOException {
		
		sendHeader(srcFileName, dstFileName, opType);
		
		RandomAccessFile raf = null;
		FileChannel fChannel = null;
		long position = 0;
		
		try{
			raf = new RandomAccessFile(new File(srcFileName), "r");
			fChannel = raf.getChannel();
			System.out.println("file size:" + fChannel.size() + " bytes");
		
			while(position < fChannel.size()) {
			    long transferedByte = fChannel.transferTo(position, fChannel.size(), client);
			    position += transferedByte;
			    System.out.println("transfer byte:"+transferedByte);
			}
			fChannel.force(false);
		}catch(IOException ex) {
			throw ex;
		} finally {
		    raf.close();
		    fChannel.close();
		    client.close();
		}
		return position;
	}
	
	/**
	 * 传送消息头
	 * @param srcFileName
	 * @param dstFileName
	 * @param opType
	 * @throws IOException
	 */
	private void sendHeader(String srcFileName, String dstFileName, int opType) throws IOException{
		byte[] magic = biginttobytes(MAGIC);
		File file = new File(srcFileName);
		int fileType = file.isDirectory()?MessageHeader. FILE_TYPE_DIRECTORY:MessageHeader.FILE_TYPE_FILE;
		byte opTypeAndFileType = (byte)((opType << 4) +  fileType);
		
		byte fileNameLen = (byte)dstFileName.length();
		byte[] bRealFileName = dstFileName.getBytes();
		byte[] bTidyFileName = new byte[255];
		System.arraycopy(bRealFileName, 0, bTidyFileName, 0, bRealFileName.length);
		byte[] bodyLen = biglongtobytes(file.length());
		
		byte[] header = new byte[269];
		System.arraycopy(magic, 0, header, 0, 4);
		System.arraycopy(new byte[]{opTypeAndFileType}, 0, header, 4, 1);
		System.arraycopy(new byte[]{fileNameLen}, 0, header, 5, 1);
		System.arraycopy(bodyLen, 0, header, 6, 8);
		System.arraycopy(bTidyFileName, 0, header, 14, bTidyFileName.length);
		
		
		ByteBuffer writeBuffer = ByteBuffer.allocate(header.length);
		System.out.println("write "+writeBuffer.remaining() +" bytes");
		writeBuffer.put(header);
		client.write(writeBuffer);
		writeBuffer.flip();
		while (writeBuffer.hasRemaining()) {
			client.write(writeBuffer);
		}
		writeBuffer.clear();
	}
	
	private static byte[] biginttobytes(int value) {  
        byte[] stream = new byte[4];
        for (int i = 0; i < 4; i++) {  
            stream[i] = (byte) ((value & (0xFF << (4 - i - 1) * 8)) >> ((4 - i - 1) * 8));  
        }  
        //outputHex(stream, 16);  
        return stream;  
    }  
	
	private static byte[] biglongtobytes(long value) {  
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.putLong(0, value);  
	    return buffer.array();    
    } 
	
	public static void main(String[] args) {
		if(args.length < 3) {
			System.out.println("usage: FtaClient host srcFile dstFile");
			return;
		}
		
		long startTime = System.currentTimeMillis();
		FtaClient client = new FtaClient(args[0], 10019);
		client.copyAndCreateFile(args[1], args[2]);
		System.out.println(System.currentTimeMillis() - startTime);
	}
	
}
