

import com.skyhawk.league.controller.*;
import com.skyhawk.league.repository.*;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;

public class ApplicationMain {
    public static void main(String[] args) {
        try {
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");

            Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            System.out.println("✅ Connected to PostgreSQL");

            // Create repositories and tables
            new LeagueRepository(connection).createTable();
            new TeamRepository(connection).createTable();
            new PlayerRepository(connection).createTable();
            new GameRepository(connection).createTable();
            new StatisticTypeRepository(connection).createTable();
            new StatisticRepository(connection).createTable();

            // Create controllers
            LeagueController leagueController = new LeagueController(connection);
            TeamController teamController = new TeamController(connection);
            PlayerController playerController = new PlayerController(connection);
            GameController gameController = new GameController(connection);
            StatisticTypeController statisticTypeController = new StatisticTypeController(connection);
            EventController eventController = new EventController(connection);

            // Start HTTP server
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

            // Register endpoints (simple routing logic)
            server.createContext("/league", exchange -> leagueController.handle(exchange));
            server.createContext("/events", exchange -> eventController.handle(exchange));
            server.createContext("/", (HttpExchange exchange) -> {
                String path = exchange.getRequestURI().getPath();
                if (path.matches("/league/[^/]+/teams/?")) {
                    teamController.handle(exchange);
                } else if (path.matches("/league/[^/]+/teams/[^/]+/players/?")) {
                    playerController.handle(exchange);
                } else if (path.matches("/league/[^/]+/games/?")) {
                    gameController.handle(exchange);
                } else if (path.matches("/league/[^/]+/statistic-types/?")) {
                    statisticTypeController.handle(exchange);
                } else {
                    exchange.sendResponseHeaders(404, -1);
                }
            });


            // Health check
            server.createContext("/health", exchange -> {
                if ("GET".equals(exchange.getRequestMethod())) {
                    String response = "OK";
                    exchange.sendResponseHeaders(200, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.getResponseBody().close();
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            });

            server.setExecutor(null);
            server.start();
            System.out.println("🚀 League app started on http://localhost:8080");

        } catch (Exception e) {
            System.err.println("❌ Error starting application: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
