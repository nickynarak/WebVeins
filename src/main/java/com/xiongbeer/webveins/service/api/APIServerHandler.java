package com.xiongbeer.webveins.service.api;


import com.xiongbeer.webveins.Configuration;
import com.xiongbeer.webveins.api.Command;
import com.xiongbeer.webveins.api.OutputFormatter;
import com.xiongbeer.webveins.api.info.FilterInfo;
import com.xiongbeer.webveins.api.info.TaskInfo;
import com.xiongbeer.webveins.api.info.WorkerInfo;
import com.xiongbeer.webveins.api.job.HDFSJob;
import com.xiongbeer.webveins.api.job.TaskJob;
import com.xiongbeer.webveins.api.jsondata.JData;
import com.xiongbeer.webveins.saver.HDFSManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by shaoxiong on 17-5-13.
 */
public class APIServerHandler extends ChannelInboundHandlerAdapter {
    private CuratorFramework client;
    private HDFSManager hdfsManager;

    public APIServerHandler(CuratorFramework zk, HDFSManager hdfsManager){
        this.client = zk;
        this.hdfsManager = hdfsManager;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String[] args = ((String)msg).split(" ");
        Command result = analysis((String) msg);
        byte[] content = (operation(result, args)
                + System.getProperty("line.separator"))
                .getBytes();
        ByteBuf message = Unpooled.buffer(content.length);
        message.writeBytes(content);
        ctx.writeAndFlush(message);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    private Command analysis(String req){
        EnumSet<Command> commands = EnumSet.allOf(Command.class);
        for(Command command:commands){
            Pattern pattern = Pattern.compile(command.toString());
            Matcher matcher = pattern.matcher(req.toUpperCase());
            while(matcher.find()){
                return command;
            }
        }
        return null;
    }

    private String operation(Command command, String... args){
        List<JData> dataSet = null;
        String result = null;
        if(command == null){
            return "[Error] Empty input";
        }
        switch (command){
            case LISTTASKS:
                TaskInfo taskInfo  = new TaskInfo(client);
                dataSet = taskInfo.getCurrentTasks().getInfo();
                result = JDecoder(dataSet);
                break;
            case LISTFILTERS:
                FilterInfo filterInfo = new FilterInfo(hdfsManager);
                try {
                    dataSet = filterInfo
                            .getBloomCacheInfo(Configuration.BLOOM_BACKUP_PATH)
                            .getInfo();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                result = JDecoder(dataSet);
                break;
            case LISTWORKERS:
                WorkerInfo workerInfo = new WorkerInfo(client);
                dataSet = workerInfo.getCurrentWoker().getInfo();
                result = JDecoder(dataSet);
                break;
            case REMOVETASKS:
                TaskJob taskJob = new TaskJob(client, hdfsManager);
                if(args.length >= 2) {
                    result += taskJob.removeTasks(args[1]);
                }
                else{
                    return "[Error] lack of args";
                }
                result += "Done.";
                break;
            case EMPTYHDFSTRASH:
                HDFSJob hdfsJob = new HDFSJob(hdfsManager);
                hdfsJob.EmptyTrash();
                result = "Done.";
                break;
            default:
                break;
        }
        return result;
    }

    private String JDecoder(List<JData> dataSet){
        if(dataSet == null|| dataSet.size() == 0){
            return "Null dataSet";
        }
        return new OutputFormatter(dataSet).format();
    }
}
