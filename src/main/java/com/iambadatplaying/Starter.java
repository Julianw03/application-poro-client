package com.iambadatplaying;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iambadatplaying.data.DataManager;
import com.iambadatplaying.frontendHandler.FrontendMessageHandler;
import com.iambadatplaying.lcuHandler.ConnectionManager;
import com.iambadatplaying.lcuHandler.SocketClient;
import com.iambadatplaying.ressourceServer.Server;
import com.iambadatplaying.tasks.ReworkedTaskManager;

import java.awt.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

public class Starter {
    public static boolean isDev = false;

    public static final int VERSION_MAJOR = 0;
    public static final int VERSION_MINOR = 1;
    public static final int VERSION_PATCH = 8;

    public static int DEBUG_FRONTEND_PORT    = 3000;
    public static int DEBUG_FRONTEND_PORT_V2 = 3001;
    public static int RESOURCE_SERVER_PORT   = 35199;

    private static String   appDirName         = "poroclient";
    public static  Starter  instance           = null;
    public static  String[] requiredEndpoints  = new String[]{"OnJsonApiEvent"};
    private static boolean  shutdownHookCalled = false;

    static HashMap<String, Consumer<String>>           simpleParameters  = new HashMap<>();
    static HashMap<String, BiConsumer<String, String>> complexParameters = new HashMap<>();

    static {
        simpleParameters.put("--dev", (s) -> {
            System.out.println("Running in developer mode");
            isDev = true;
        });

        complexParameters.put("--app-port", (s, v) -> {
            try {
                int port = Integer.parseInt(v);
                if (port < 0 || port > 65535) {
                    throw new NumberFormatException();
                }
                System.out.println("Setting app port to: " + port);
                RESOURCE_SERVER_PORT = port;
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number: " + v);
            }
        });

        complexParameters.put("--debug-port", (s, v) -> {
            try {
                int port = Integer.parseInt(v);
                if (port < 0 || port > 65535) {
                    throw new NumberFormatException();
                }
                System.out.println("Setting debug port to: " + port);
                DEBUG_FRONTEND_PORT = port;
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number: " + v);
            }
        });
    }

    private Path taskDirPath = null;
    private Path basePath    = null;

    private SocketClient           client;
    private ConnectionManager      connectionManager;
    private FrontendMessageHandler frontendMessageHandler;

    private Server              server;
    private ReworkedTaskManager taskManager;

    private ConfigLoader configLoader;

    private ConnectionStatemachine connectionStatemachine;

    private DataManager reworkedDataManager;

    public static Starter getInstance() {
        if (instance == null) {
            instance = new Starter();
        }
        return instance;
    }

    public static void main(String[] args) {
        readPassedParameters(args);
        Starter starter = Starter.getInstance();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Starter.shutdownHookCalled = true;
            Starter.getInstance().getConnectionStatemachine().transition(ConnectionStatemachine.State.STOPPING);
        }));
        starter.connectionStatemachine = new ConnectionStatemachine(starter);
        starter.run();
    }

    private static void readPassedParameters(String[] args) {
        for (String arg : args) {
            if (!arg.startsWith("--")) continue;
            String[] split = arg.split(Pattern.quote("="));
            switch (split.length) {
                case 1:
                    simpleParameters.getOrDefault(split[0], (s) -> {
                        System.out.println("Unknown parameter: " + s);
                    }).accept(split[0]);
                    break;
                case 2:
                    complexParameters.getOrDefault(split[0], (s, v) -> {
                        System.out.println("Unknown parameter: " + s);
                    }).accept(split[0], split[1]);
                    break;
                default:
                    System.out.println("Invalid parameter: " + arg);
            }
        }
    }

    public static String getAppDirName() {
        return appDirName;
    }

    public void run() {
        if (isShutdownPending()) return;
        logPreRunMessages();
        initReferences();
        configLoader.loadConfig();
        server.init();
        connectionManager.init();
        openClient();
        connectionStatemachine.transition(ConnectionStatemachine.State.AWAITING_LEAGUE_PROCESS);
    }

    private void logPreRunMessages() {
        if (isDev) {
            log("--------------------------------------------------------------------", LOG_LEVEL.WARN);
            log("RUNNING DEVELOPER VERSION; SOME SECURITY FEATURES MAY NOT BE ACTIVE!", LOG_LEVEL.WARN);
            log("--------------------------------------------------------------------", LOG_LEVEL.WARN);
        }
        log("Running Poro-Client Version " + VERSION_MAJOR + "." + VERSION_MINOR + "." + VERSION_PATCH, LOG_LEVEL.INFO);
        if (VERSION_MAJOR == 0) {
            log("This is an alpha Version, bugs may still be present!", LOG_LEVEL.WARN);
        }
        if (isDev) {
            log("Available Logging:");
            for (LOG_LEVEL l : LOG_LEVEL.values()) {
                log("", l);
            }
        }
    }

    private void openClient() {
        URI uri;
        if (Starter.isDev) {
            uri = URI.create("http://127.0.0.1:" + DEBUG_FRONTEND_PORT);
        } else {
            uri = URI.create("http://127.0.0.1:" + RESOURCE_SERVER_PORT + "/static");
        }
        try {
            Desktop.getDesktop().browse(uri);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void leagueProcessReady() {
        client.init();
        reworkedDataManager.init();
        taskManager.init();
        server.getLocalWebSockets().forEach(
                socket -> frontendMessageHandler.sendInitialData(socket)
        );
        subscribeToEndpointsOnConnection();
    }

    private void initReferences() {
        configLoader = new ConfigLoader(this);
        server = new Server(this);
        connectionManager = new ConnectionManager(this);
        reworkedDataManager = new DataManager(this);
        client = new SocketClient(this);
        frontendMessageHandler = new FrontendMessageHandler(this);
        taskManager = new ReworkedTaskManager(this);
    }


    private void subscribeToEndpointsOnConnection() {
        new Thread(() -> {
            while (getClient().getSocket() == null || !getClient().getSocket().isConnected()) {
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            for (String endpoint : requiredEndpoints) {
                getClient().getSocket().subscribeToEndpoint(endpoint);
            }
        }).start();
    }

    public void backendMessageReceived(String message) {
        if (message != null && !message.isEmpty()) {
            if (connectionStatemachine.getCurrentState() != ConnectionStatemachine.State.CONNECTED) return;
            JsonElement messageElement = JsonParser.parseString(message);
            JsonArray messageArray = messageElement.getAsJsonArray();
            if (messageArray.isEmpty()) return;
            JsonObject dataPackage = messageArray.get(2).getAsJsonObject();
            if (!Util.jsonKeysPresent(dataPackage, "data", "uri", "eventType")) return;
            final JsonElement data = dataPackage.get("data");
            final String uri = dataPackage.get("uri").getAsString();
            final String type = dataPackage.get("eventType").getAsString();
            if (message.length() >= 100_000) {
                log(type + " " + uri + ": - Too long to print - ", Starter.LOG_LEVEL.LCU_MESSAGING);
            } else {
                log(type + " " + uri + ": " + message, Starter.LOG_LEVEL.LCU_MESSAGING);
            }
            new Thread(() -> getDataManager().update(type, uri, data)).start();
            new Thread(() -> getTaskManager().updateAllTasks(type, uri, data)).start();
        }
    }

    public void shutdown() {
        configLoader.saveConfig();
        resetAllInternal();
        log("Shutting down");
        if (!shutdownHookCalled) System.exit(0);
    }

    public void handleLCUDisconnect() {
        connectionManager.setLeagueAuthDataAvailable(false);
        server.resetCachedData();
        resetLCUDependentComponents();
    }

    private void resetLCUDependentComponents() {
        client.shutdown();
        reworkedDataManager.shutdown();
        taskManager.shutdown();
    }

    public void resetAllInternal() {
        if (isShutdownPending()) {
            server.shutdown();
            taskManager.shutdown();
            client.shutdown();
            reworkedDataManager.shutdown();
            connectionManager.shutdown();
            server = null;
            taskManager = null;
            client = null;
            connectionManager = null;
        }
    }

    public void log(String s, LOG_LEVEL level) {
        if (!isDev) {
            switch (level) {
                case ERROR:
                case INFO:
                case WARN:
                    break;
                case DEBUG:
                case LCU_MESSAGING:
                case NO_PARSING:
                default:
                    return;
            }
        } else {
            switch (level) {
                case ERROR:
                case INFO:
                case WARN:
                case DEBUG:
                case LCU_MESSAGING:
                case NO_PARSING:
                    break;
                default:
                    return;
            }
        }
        String prefix = "[" + level.name() + "]";
        switch (level) {
            case WARN:
                prefix = "\u001B[35m" + prefix + "\u001B[0m";
                break;
            case ERROR:
                prefix = "\u001B[31m" + prefix + "\u001B[0m";
                break;
            case DEBUG:
                prefix = "\u001B[32m" + prefix + "\u001B[0m";
                break;
            case LCU_MESSAGING:
                prefix = "\u001B[34m" + prefix + "\u001B[0m";
                break;
            case NO_PARSING:
                prefix = "\u001B[37m" + prefix + "\u001B[0m";
                break;
            case INFO:
                prefix = "\u001B[33m" + prefix + "\u001B[0m";
                break;
        }
        System.out.println(prefix + ": " + s);
    }

    public void log(String s) {
        log(s, LOG_LEVEL.DEBUG);
    }

    public Path getTaskPath() {
        if (taskDirPath == null) {
            taskDirPath = getConfigLoader().getAppFolderPath().resolve(ConfigLoader.USER_DATA_FOLDER_NAME).resolve(ConfigLoader.TASKS_FOLDER_NAME);
        }
        return taskDirPath;
    }

    public Path getBasePath() {
        if (basePath == null) {
            try {
                URL location = this.getClass().getProtectionDomain().getCodeSource().getLocation();
                Path currentDirPath = Paths.get(location.toURI()).getParent();
                basePath = currentDirPath;
                log("Base-Location: " + currentDirPath, LOG_LEVEL.INFO);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return basePath;
    }

    public ReworkedTaskManager getTaskManager() {
        return taskManager;
    }

    public SocketClient getClient() {
        return client;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public FrontendMessageHandler getFrontendMessageHandler() {
        return frontendMessageHandler;
    }

    public DataManager getDataManager() {
        return reworkedDataManager;
    }

    public ConfigLoader getConfigLoader() {
        return configLoader;
    }

    public ConnectionStatemachine getConnectionStatemachine() {
        return connectionStatemachine;
    }

    public Server getServer() {
        return server;
    }

    public boolean isShutdownPending() {
        if (connectionStatemachine == null) return false;
        return connectionStatemachine.getCurrentState() == ConnectionStatemachine.State.STOPPING;
    }

    public boolean isInitialized() {
        if (connectionStatemachine == null) return false;
        return connectionStatemachine.getCurrentState() == ConnectionStatemachine.State.CONNECTED;
    }

    public enum LOG_LEVEL {
        NO_PARSING,
        LCU_MESSAGING,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    public enum EXIT_CODE {
        INSUFFICIENT_PERMISSIONS(401, "Insufficient Permissions"),
        CERTIFICATE_SETUP_FAILED(495, "Certificate Setup Failed"),
        HTTP_PATCH_SETUP_FAILED(505, "HTTP Patch Setup Failed"),
        MULTIPLE_CONNECTION_ATTEMPTS_FAILED(522, "Multiple Connection Attempts Failed"),
        SERVER_BIND_FAILED(500, "Server Bind Failed");

        private       int    code;
        private final String message;

        EXIT_CODE(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    public void exit(EXIT_CODE code) {
        log("Exiting with code: " + code.code + " - " + code.message, LOG_LEVEL.ERROR);
        System.exit(code.code);
    }
}
