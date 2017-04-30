package com.xiongbeer;

import java.io.IOException;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import com.xiongbeer.service.Server;
import com.xiongbeer.zk.worker.Worker;

public class WebVeinsServer implements Watcher {
	private Server server;
	private Worker worker;
	private String serverId;
	private ZooKeeper zk;
	private Configuration configuration;
	private static WebVeinsServer wvServer;
	
	private WebVeinsServer() throws IOException {
    	configuration = Configuration.getInstance();
        zk = new ZooKeeper(Configuration.ZOOKEEPER_INIT_SERVER, 1000, this);
        serverId = new IdProvider().getId();
        worker = new Worker(zk, serverId);
    }
    
    public static synchronized WebVeinsServer getInstance() throws IOException {
        if(wvServer == null){
        	wvServer = new WebVeinsServer();
        }
        return wvServer;
    }
    
    public void stopServer(){
        server.stop();
    }
    
    private void runServer() throws IOException {
        server = new Server(Configuration.LOCAL_HOST,
                Configuration.LOCAL_PORT, worker.getTaskWorker());
        server.bind();
    }
    
    @Override
	public void process(WatchedEvent arg0) {}
    
    public static void main(String[] args) throws IOException {
        InitLogger.init();
        WebVeinsServer server = WebVeinsServer.getInstance();
        server.runServer();;
    }
}
