package com.jnetty.core.server;

import com.jnetty.core.Config.ServiceConfig;
import com.jnetty.core.connector.Connector;
import com.jnetty.core.server.handler.NettyHandler;
import com.jnetty.util.log.JNettyLogger;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

public class SimpleNettyServer implements Server {

	private EventLoopGroup workerGroup = null;
	private EventLoopGroup bossGroup = null;
	private ServerBootstrap serverBootstrap = null;

	private ServiceConfig serviceConfig = null;
	private Connector connector = null;
	private ChannelFuture future = null;
	private ChannelFuture serverChannelFuture = null;
	
	public void initialize() {
		this.workerGroup = new NioEventLoopGroup();
		this.bossGroup = new NioEventLoopGroup();
		this.serverBootstrap =  new ServerBootstrap();
		this.serverBootstrap.group(bossGroup, workerGroup);
		this.serverBootstrap.channel(NioServerSocketChannel.class);
		this.serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel sc) throws Exception {
//				if (SimpleNettyServer.this.serviceConfig.useSSL) {
//					SSLEngine engine = new MySSlEngine();
//		            engine.setUseClientMode(false);
//		            sc.pipeline.addLast("ssl", new SslHandler(engine));
//				}
				sc.pipeline().addLast("http-decoder", new HttpRequestDecoder());
				sc.pipeline().addLast("http-aggregator", new HttpObjectAggregator(65536));
				sc.pipeline().addLast("http-encoder", new HttpResponseEncoder());
				sc.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
				NettyHandler nettyHandler = new NettyHandler();
				nettyHandler.setServer(SimpleNettyServer.this);
				sc.pipeline().addLast("netty-handler", nettyHandler);
			}
		});
		this.serverBootstrap.option(ChannelOption.SO_BACKLOG, this.serviceConfig.so_back_log);
		this.serverBootstrap.option(ChannelOption.SO_KEEPALIVE, this.serviceConfig.so_keep_alive);
	}

	public void start() {
		try {
			future = this.serverBootstrap.bind(this.connector.getIp(), this.connector.getPort()).sync();
			serverChannelFuture = future.channel().closeFuture();
			serverChannelFuture.sync();
		} catch (InterruptedException e) {
			JNettyLogger.log(e);
		} finally {
			this.bossGroup.shutdownGracefully();
			this.workerGroup.shutdownGracefully();
		}
	}

	public void stop() {
		serverChannelFuture.channel().close();
	}

	public ServiceConfig getConfig() {
		return this.serviceConfig;
	}

	public void setConfig(ServiceConfig config) {
		this.serviceConfig = config;
	}

	public void setParent(Connector connector) {
		this.connector = connector;
	}

	public Connector getParent() {
		return this.connector;
	}
	
}
