package com.bc.algorithm.poxas;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EasyNio {
	private final static Logger logger = LoggerFactory.getLogger(EasyNio.class);

	public static void server(int port, Callback callBack) {
		ServerSocketChannel channel = null;
		Selector selector = null;
		try {
			channel = ServerSocketChannel.open();
			selector = Selector.open();
			channel.configureBlocking(false);
			channel.socket().setReuseAddress(true);
			channel.bind(new InetSocketAddress(port));
			channel.register(selector, SelectionKey.OP_ACCEPT);
			Map<SocketChannel, Object> cache = new HashMap<>();
			while (true) {
				if (selector.select() > 0) {
					Set<SelectionKey> sets = selector.selectedKeys();
					Iterator<SelectionKey> keys = sets.iterator();
					int i = 0;
					while (keys.hasNext()) {
						SelectionKey key = keys.next();
						keys.remove();
						try {
							if (key.isAcceptable()) {
								SocketChannel schannel = ((ServerSocketChannel) key.channel()).accept();
								schannel.configureBlocking(false);
								schannel.register(selector, SelectionKey.OP_READ);
							}
							if (key.isReadable()) {
								SocketChannel schannel = (SocketChannel) key.channel();
								ByteBuffer buf = ByteBuffer.allocate(1024);
								ByteArrayOutputStream output = new ByteArrayOutputStream();
								int len = 0;
								while ((len = schannel.read(buf)) > 0) {
									buf.flip();
									byte by[] = new byte[buf.remaining()];
									buf.get(by);
									output.write(by);
									buf.clear();
								}
								String str = new String(output.toByteArray());
								Object resp = callBack.execute(str);
								cache.put(schannel, resp);
								schannel.register(selector, SelectionKey.OP_WRITE);
							}
							if (key.isWritable()) {
								SocketChannel schannel = (SocketChannel) key.channel();
								String attach = (String) cache.get(schannel);
								schannel.write(ByteBuffer.wrap(attach.getBytes()));
								schannel.close();
								key.cancel();
							}
						} catch (Exception e) {
							key.cancel();
							logger.error("", e);
						}
					}
				}
			}
		} catch (IOException e) {
			logger.error("", e);
		} finally {
			try {
				if (channel != null) {
					channel.close();
				}
			} catch (IOException e) {
				logger.error("", e);
			}
		}
	}

	public static void client(int port, Object request, Callback callBack) {
		SocketChannel channel = null;
		Selector selector = null;
		try {
			channel = SocketChannel.open();
			selector = Selector.open();
			channel.configureBlocking(false);
			channel.connect(new InetSocketAddress(port));
			channel.register(selector, SelectionKey.OP_CONNECT);
			while (selector.select() > 0) {
				Iterator<SelectionKey> set = selector.selectedKeys().iterator();
				while (set.hasNext()) {
					SelectionKey key = set.next();
					set.remove();
					SocketChannel ch = (SocketChannel) key.channel();
					try {
						if (key.isConnectable()) {
							if (ch.isConnectionPending()) {
								ch.finishConnect();
							}
							ch.configureBlocking(false);
							ch.register(selector, SelectionKey.OP_WRITE);
						}

						if (key.isReadable()) {
							ByteArrayOutputStream out = new ByteArrayOutputStream();
							ByteBuffer buffer = ByteBuffer.allocate(1024);
							int len = 0;
							while ((len = ch.read(buffer)) > 0) {
								buffer.flip();
								byte by[] = new byte[buffer.remaining()];
								buffer.get(by);
								out.write(by);
								buffer.clear();
							}
							callBack.execute(new String(out.toByteArray()));
							out.close();
							channel.close();
							key.cancel();
							return;
						}
						if (key.isWritable()) {
							ch.write(ByteBuffer.wrap(((String) request).getBytes()));
							ch.register(selector, SelectionKey.OP_READ);
						}
					} catch (Exception e) {
						logger.error("", e);
					}
				}
			}

		} catch (Exception e) {
			logger.error("", e);
		} finally {
			try {
				if (channel != null) {
					channel.close();
				}
			} catch (IOException e) {
				logger.error("", e);
			}
		}
	}

	public static interface Callback {
		Object execute(Object para);
	}

}
