/*
 * Copyright 2015 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.examples.helloworld;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.helloworld.proto.HelloReply;
import io.grpc.examples.helloworld.proto.HelloRequest;
import io.grpc.examples.helloworld.proto.HelloWorldGrpc;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class HelloWorldClient {
    private static final Logger logger = Logger.getLogger(HelloWorldClient.class.getName());

    private final HelloWorldGrpc.HelloWorldBlockingStub blockingStub;

    public HelloWorldClient(Channel channel) {
        blockingStub = HelloWorldGrpc.newBlockingStub(channel);
    }

    public void send() {
        HelloRequest request = HelloRequest.newBuilder().setHello("Hello").setWorld("world").build();
        Iterator<HelloReply> responses;
        try {
            responses = blockingStub.sayHello(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
        while (responses.hasNext()){
            logger.info("return message: " + responses.next().getMessage());
        }
    }


    public static void main(String[] args) throws Exception {
        String target = "localhost:50051";
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
                                                      .usePlaintext()
                                                      .build();
        try {
            HelloWorldClient client = new HelloWorldClient(channel);
            client.send();
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
