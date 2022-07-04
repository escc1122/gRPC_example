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

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.examples.helloworld.proto.HelloReply;
import io.grpc.examples.helloworld.proto.HelloRequest;
import io.grpc.examples.helloworld.proto.HelloWorldGrpc;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HelloWorldServer {
    private static final Logger logger = Logger.getLogger(HelloWorldServer.class.getName());

    private Server server;

    private void start() throws IOException {
        int port = 50051;
        server = ServerBuilder.forPort(port)
                              .addService(new HelloWorldImpl())
                              .build()
                              .start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    HelloWorldServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        final HelloWorldServer server = new HelloWorldServer();
        server.start();
        server.blockUntilShutdown();
    }

    static class HelloWorldImpl extends HelloWorldGrpc.HelloWorldImplBase {
        @Override
        public StreamObserver<HelloRequest> sayHello(final StreamObserver<HelloReply> responseObserver) {
            return new StreamObserver<HelloRequest>() {
                private int count = 0;

                @Override
                public void onNext(HelloRequest helloRequest) {
                    String hello = helloRequest.getHello();
                    String world = helloRequest.getWorld();
                    logger.info("req.getHello() : " + hello);
                    logger.info("req.getWorld() : " + world);
                    String message = hello + " " + world + " " + count++;
                    responseObserver.onNext(HelloReply.newBuilder().setMessage(message).build());
                    responseObserver.onNext(HelloReply.newBuilder().setMessage("server stream").build());
                }

                @Override
                public void onError(Throwable throwable) {
                    logger.log(Level.WARNING, "Failed: {0}", Status.fromThrowable(throwable));
                }

                @Override
                public void onCompleted() {
                    logger.info("onCompleted");
                    responseObserver.onCompleted();
                }
            };
        }

    }

}
