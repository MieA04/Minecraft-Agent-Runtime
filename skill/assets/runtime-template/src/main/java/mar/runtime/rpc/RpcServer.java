package mar.runtime.rpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import mar.runtime.session.SessionManager;
import mar.runtime.thread.ExecutionTarget;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class RpcServer implements AutoCloseable {
    private final String host;
    private final int configuredPort;
    private final ObjectMapper json = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private final RpcDispatcher dispatcher;
    private final ExecutorService connections;
    private final Set<Socket> openSockets = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean();

    private volatile ServerSocket serverSocket;
    private volatile Thread acceptThread;

    public RpcServer(
            String host,
            int configuredPort,
            SessionManager sessions,
            ExecutionTarget rawTarget,
            ExecutionTarget clientTarget,
            ExecutionTarget serverTarget) {
        this.host = host;
        this.configuredPort = configuredPort;
        this.dispatcher = new RpcDispatcher(sessions, rawTarget, clientTarget, serverTarget);
        this.connections = Executors.newCachedThreadPool(daemonFactory("mar-rpc-connection-"));
    }

    public synchronized void start() {
        if (serverSocket != null) {
            return;
        }
        if (closed.get()) {
            throw new IllegalStateException("Cannot start a closed MAR RPC server");
        }

        try {
            InetAddress address = InetAddress.getByName(host);
            if (!address.isLoopbackAddress()) {
                throw new IllegalStateException("MAR RPC server may only bind loopback: " + host);
            }
            ServerSocket socket = new ServerSocket();
            try {
                socket.bind(new InetSocketAddress(address, configuredPort));
            } catch (Throwable error) {
                try {
                    socket.close();
                } catch (IOException closeError) {
                    error.addSuppressed(closeError);
                }
                throw error;
            }
            serverSocket = socket;
            acceptThread = daemonFactory("mar-rpc-accept-").newThread(this::acceptLoop);
            acceptThread.start();
        } catch (IOException error) {
            throw new IllegalStateException(
                    "Cannot bind MAR RPC server to " + host + ":" + configuredPort, error);
        }
    }

    public String host() {
        return host;
    }

    public int port() {
        ServerSocket current = serverSocket;
        if (current == null) {
            throw new IllegalStateException("MAR RPC server has not started");
        }
        return current.getLocalPort();
    }

    private void acceptLoop() {
        while (!closed.get()) {
            try {
                Socket socket = serverSocket.accept();
                openSockets.add(socket);
                try {
                    connections.execute(new RpcConnection(
                            socket, json, dispatcher, () -> openSockets.remove(socket)));
                } catch (RejectedExecutionException error) {
                    openSockets.remove(socket);
                    socket.close();
                }
            } catch (SocketException error) {
                if (!closed.get()) {
                    close();
                }
                return;
            } catch (IOException error) {
                if (!closed.get()) {
                    close();
                }
                return;
            }
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        ServerSocket listener = serverSocket;
        if (listener != null) {
            try {
                listener.close();
            } catch (IOException ignored) {
                // Continue closing active connections.
            }
        }
        for (Socket socket : openSockets) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // Continue closing remaining connections.
            }
        }
        openSockets.clear();
        connections.shutdownNow();
        try {
            connections.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }

    private static ThreadFactory daemonFactory(String prefix) {
        AtomicLong sequence = new AtomicLong(1);
        return task -> {
            Thread thread = new Thread(task, prefix + sequence.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }
}
