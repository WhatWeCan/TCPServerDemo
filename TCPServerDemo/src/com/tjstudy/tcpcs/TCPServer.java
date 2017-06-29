package com.tjstudy.tcpcs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * 服务器端：时刻准备着让客户端连接，接收到客户端连接之后，读取客户端发送来的信息，并返回数据到客户端
 * 
 * @author Administrator
 * 
 */
public class TCPServer {
	private static Selector serverSelector;
	private static ByteBuffer serBuffer;

	public static void main(String[] args) throws IOException {
		// 1、获取服务端Channel
		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		// 服务器端绑定到指定的端口
		serverSocketChannel.socket().bind(new InetSocketAddress(8888));

		// 2、开启服务端 信道
		serverSelector = Selector.open();
		serverSocketChannel.configureBlocking(false);// must 设置为不阻塞
		serverSocketChannel.register(serverSelector, SelectionKey.OP_ACCEPT);// 初始状态，设置为可accept的
		serBuffer = ByteBuffer.allocate(1024);

		new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {// 外层循环 保证服务器一直处于运行状态
					try {
						int select = serverSelector.select();
						if (select == 0) {
							// System.out.println("select:" + select);
							continue;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					// 内层需要一直读取信道的值 进行相应操作
					Iterator<SelectionKey> sKeys = serverSelector
							.selectedKeys().iterator();
					while (sKeys.hasNext()) {
						SelectionKey key = sKeys.next();
						if (key.isAcceptable()) {
							System.out.println("key.isAcceptable() 客户端连接");
							// 每一个信道可以处理多个channel
							ServerSocketChannel serverChannel = (ServerSocketChannel) key
									.channel();
							try {
								SocketChannel clientChannel = serverChannel
										.accept();
								// 将客户端Channel绑定到信道
								clientChannel.configureBlocking(false);
								clientChannel.register(serverSelector,
										SelectionKey.OP_READ);// 要读取里面的数据
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} else if (key.isReadable()) {
							String s = "";
							// 读取数据
							serBuffer.clear();
							SocketChannel clientChannel = (SocketChannel) key
									.channel();
							try {
								int read = clientChannel.read(serBuffer);
								if (read == -1) {
									System.out.println("read==-1");
									continue;
								}
								serBuffer.flip();
								s = new String(serBuffer.array(), 0, serBuffer
										.limit());
								System.out.println("服务器接收到数据：" + s);

								// 更改信道模式为可写模式
								clientChannel.configureBlocking(false);
								clientChannel.register(serverSelector,
										SelectionKey.OP_WRITE);
							} catch (IOException e) {
								try {
									clientChannel.close();
									key.cancel();
								} catch (IOException e1) {
									e1.printStackTrace();
								}
								// e.printStackTrace();
							}
						} else if (key.isWritable() && key.isValid()) {
							SocketChannel clientChannel = (SocketChannel) key
									.channel();
							serBuffer.clear();
							serBuffer.put("$$".getBytes());
							byte[] len = short2Byte(12);
							serBuffer.put(len);
							serBuffer.put("tjstudy".getBytes());
							serBuffer.flip();
							try {
								clientChannel.write(serBuffer);
								// 切换成读模式，否则将会一直是写模式
								clientChannel.configureBlocking(false);
								clientChannel.register(serverSelector,
										SelectionKey.OP_READ);// 要读取里面的数据
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								try {
									clientChannel.close();
									key.cancel();
								} catch (IOException e1) {
									e1.printStackTrace();
								}
							}
						}
						sKeys.remove();
					}
				}
			}
		}).start();
	}

	/**
	 * 将short转换为byte数组
	 * 
	 * @param s
	 *            short
	 * @return
	 */
	private static byte[] short2Byte(int s) {
		byte[] shortBuf = new byte[2];
		for (int i = 0; i < 2; i++) {
			int offset = (shortBuf.length - 1 - i) * 8;
			shortBuf[1 - i] = (byte) ((s >>> offset) & 0xff);
		}
		return shortBuf;
	}
}
