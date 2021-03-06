/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.at;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import org.pcj.AsyncTask;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.PcjThread;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class AsyncAtRequestMessage<T> extends Message {

    private int requestNum;
    private int groupId;
    private int requesterThreadId;
    private int threadId;
    private AsyncTask<T> asyncTask;

    public AsyncAtRequestMessage() {
        super(MessageType.ASYNC_AT_REQUEST);
    }

    public AsyncAtRequestMessage(int groupId, int requestNum, int requesterThreadId, int threadId, AsyncTask<T> asyncTask) {
        this();

        this.groupId = groupId;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;
        this.threadId = threadId;
        this.asyncTask = asyncTask;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(requestNum);
        out.writeInt(requesterThreadId);
        out.writeInt(threadId);
        out.writeObject(asyncTask);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        requestNum = in.readInt();
        requesterThreadId = in.readInt();
        threadId = in.readInt();

        try {
            asyncTask = (AsyncTask<T>) in.readObject();
        } catch (Exception ex) {
            AsyncAtResponseMessage asyncAtResponseMessage = new AsyncAtResponseMessage(groupId, requestNum, requesterThreadId, ex);

            InternalPCJ.getNetworker().send(sender, asyncAtResponseMessage);
            return;
        }

        NodeData nodeData = InternalPCJ.getNodeData();
        PcjThread pcjThread = nodeData.getPcjThread(groupId, requesterThreadId);

        pcjThread.execute(() -> {
            AsyncAtResponseMessage asyncAtResponseMessage;
            try {
                T returnedValue = asyncTask.call();

                asyncAtResponseMessage = new AsyncAtResponseMessage(groupId, requestNum, requesterThreadId, returnedValue);
            } catch (Exception ex) {
                asyncAtResponseMessage = new AsyncAtResponseMessage(groupId, requestNum, requesterThreadId, ex);
            }
            InternalPCJ.getNetworker().send(sender, asyncAtResponseMessage);
        });
    }
}