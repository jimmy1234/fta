package com.tencent.code.fta;

import java.nio.ByteBuffer;

/**
 * ----------------------------------------------
 * magic    |op|type|fileLen|bodyLen |fileName |
 * -----------------------------------------------
 * 4            |1           |1         |8              |255          |
 * ----------------------------------------------
 * @author jimmyquan
 * 为了效率考虑，用定长的头，fileName固定255字节
 */
public class MessageHeader {

	//消息头magic number, 753195100
	private final static int MAGIC = 753195100;
	//包头长度，文件名长度
	private int fileNameLen;
	//消息体长度，其实就是文件内容长度
	private long bodyLen;
	//文件名
	private String fileName;
	
	//操作类型 0-删除, 1-新增, 2-覆盖
	private int opType;
	
	//文件类型 0-目录, 1-文件
	private int fileType;
	
	
	public final static int OPERATION_TYPE_CREATE = 0;
	public final static int OPERATION_TYPE_DELETE = 1;
	public final static int OPERATION_TYPE_OVERWRITING = 2;
	
	public final static int FILE_TYPE_DIRECTORY = 0;
	public final static int FILE_TYPE_FILE = 1;
	
	
	public MessageHeader(int fileNameLen, int opType, int fileType, long bodyLen) {
		this.fileNameLen = fileNameLen;
		this.opType = opType;
		this.fileType = fileType;
		this.bodyLen = bodyLen;
	}	
	
	public int getFileNameLen() {
		return fileNameLen;
	}
	
	public void setFileNameLen(int headerLen) {
		this.fileNameLen = headerLen;
	}
	
	public long getBodyLen() {
		return bodyLen;
	}
	
	public void setBodyLen(long bodyLen) {
		this.bodyLen = bodyLen;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public static boolean isValidMessage(byte[] magicByte) {
		return bytes2bigint(magicByte) == MAGIC;
	}
	
	public int getFileType() {
		return fileType;
	}


	public void setFileType(int fileType) {
		this.fileType = fileType;
	}


	public int getOpType() {
		return opType;
	}


	public void setOpType(int opType) {
		this.opType = opType;
	}  
	
	public static int bytes2bigint(byte stream[]) {  
	    int value = 0;  
	    int temp = 0;  
	    for (int i = 0; i < 4; i++)  
	    {  
	        if ((stream[i]) >= 0)  
	        {  
	            temp = stream[i];  
	        }  
	        else  
	        {  
	            temp = stream[i] + 256;  
	        }  
	        temp <<= ((4 - i - 1) * 8);  
	        value += temp;  
	    }  
	    return value;  
	}	
	
	public static long bytes2biglong(byte stream[]) {  
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.put(stream, 0, stream.length);  
	     buffer.flip();//need flip   
	     return buffer.getLong();  
	}
}
