/*
 * Copyright (c) 2016, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.test;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pcj.NodesDescription;
import org.pcj.PCJ;
import org.pcj.PcjRuntimeException;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@RegisterStorage(ExecuteAsyncAtTest.Shared.class)
public class ExecuteAsyncAtTest implements StartPoint {

    @Storage(ExecuteAsyncAtTest.class)
    enum Shared {
        v
    }

    private int v = -1;

    public static void main(String[] args) throws InterruptedException {
        Level level = Level.INFO;
//        Level level = Level.FINEST;
        Logger logger = Logger.getLogger("");
        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
        logger.setLevel(level);

        NodesDescription nodesDescription = new NodesDescription(new String[]{
                "localhost",
                "localhost",});

//        PCJ.start(EasyTest.class, EasyTest.class,
        PCJ.deploy(ExecuteAsyncAtTest.class, nodesDescription);
    }

    @Override
    public void main() throws Throwable {
        if (PCJ.myId() == 0) {
            try {
                PCJ.asyncAt(1, () -> {
                    PCJ.putLocal(PCJ.myId(), Shared.v);
                    // v = PCJ.myId; // throws java.io.NotSerializableException: org.pcj.test.ExecuteAsyncAtTest
                    System.out.println("output: " + PCJ.myId());
                    throw new RuntimeException("rzucony wyjatek");
                }).get();
            } catch (PcjRuntimeException ex) {
                System.err.println(ex.toString() + ":\n"
                                           + Arrays.stream(ex.getSuppressed())
                                                     .map(e -> "\t * " + e.toString())
                                                     .collect(Collectors.joining("\n")));
            }
        }
        PCJ.barrier();
        System.out.println("v = " + v);
    }
}
