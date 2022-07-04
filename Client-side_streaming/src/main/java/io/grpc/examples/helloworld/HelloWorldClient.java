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
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.helloworld.proto.HelloReply;
import io.grpc.examples.helloworld.proto.HelloRequest;
import io.grpc.examples.helloworld.proto.HelloWorldGrpc;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class HelloWorldClient {
    private static final Logger logger = Logger.getLogger(HelloWorldClient.class.getName());
    private final HelloWorldGrpc.HelloWorldStub asyncStub;

    public HelloWorldClient(Channel channel) {
        asyncStub = HelloWorldGrpc.newStub(channel);
    }

    public void send() throws InterruptedException {
        //流程結束前要等待
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<HelloReply> responseObserver = new StreamObserver<HelloReply>() {
            @Override
            public void onNext(HelloReply helloReply) {
                logger.info("onNext");
                logger.info("return message: " + helloReply.getMessage());
            }

            @Override
            public void onError(Throwable throwable) {
                logger.log(Level.WARNING, "Failed: {0}", Status.fromThrowable(throwable));
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                logger.info("onCompleted");
                finishLatch.countDown();
            }
        };
        StreamObserver<HelloRequest> requestObserver = asyncStub.sayHello(responseObserver);

        try {
            for (int i = 0; i < 5; i++) {
                HelloRequest request = HelloRequest.newBuilder().setHello("Hello").setWorld("world").build();
                requestObserver.onNext(request);
            }
        } catch (RuntimeException e) {
            // Cancel RPC
            requestObserver.onError(e);
            throw e;
        }
        // Mark the end of requests
        requestObserver.onCompleted();

        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            logger.warning("Can not finish within 1 minutes");
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
