import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 3000;
    private static List<PlayerHandler> players = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        System.out.println("Server in ascolto sulla porta " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (players.size() < 2) {
                Socket socket = serverSocket.accept();
                PlayerHandler player = new PlayerHandler(socket);
                players.add(player);
                new Thread(player).start();
                System.out.println("Giocatore connesso. Totale: " + players.size());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class PlayerHandler implements Runnable {
        Socket socket;
        PrintWriter out;
        BufferedReader in;
        String name;
        List<List<int[]>> ships = new ArrayList<>();
        boolean ready = false;
        boolean myTurn = false;

        public PlayerHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        @Override
        public void run() {
            try {
                String input;
                while ((input = in.readLine()) != null) {
                    handleMessage(input);
                }
            } catch (IOException e) {
                System.out.println("Connessione persa con " + name);
            } finally {
                players.remove(this);
            }
        }

        private void handleMessage(String msg) {
            if (msg.startsWith("JOIN:")) {
                this.name = msg.split(":")[1];
                out.println("MSG:Benvenuto " + name + "! Attendi l'altro giocatore.");
                out.println("JOIN_OK");
            } else if (msg.startsWith("PLACE_SHIPS:")) {
                parseShips(msg.substring(12));
                this.ready = true;
                out.println("MSG:Navi posizionate. In attesa dell'avversario...");
                checkGameStart();
            } else if (msg.startsWith("ATTACK:")) {
                handleAttack(msg.substring(7));
            }
        }

        private void parseShips(String data) {
            String[] shipGroups = data.split("\\|");
            for (String group : shipGroups) {
                List<int[]> shipPos = new ArrayList<>();
                for (String pos : group.split(";")) {
                    String[] coords = pos.split(",");
                    shipPos.add(new int[]{Integer.parseInt(coords[0]), Integer.parseInt(coords[1])});
                }
                ships.add(shipPos);
            }
        }

        private void handleAttack(String data) {
            if (!myTurn) return;
            String[] coords = data.split(",");
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);

            PlayerHandler defender = (players.get(0) == this) ? players.get(1) : players.get(0);
            boolean hit = false;
            int shipIdx = -1;
            int coordIdx = -1;

            //cerca la coordinata che è stata colpita tra le navi di chi difente
            for (int i = 0; i < defender.ships.size(); i++) {
                List<int[]> ship = defender.ships.get(i);
                for (int j = 0; j < ship.size(); j++) {
                    if (ship.get(j)[0] == x && ship.get(j)[1] == y) {
                        hit = true;
                        shipIdx = i;
                        coordIdx = j;
                        break;
                    }
                }
                if (hit) break;
            }

            String result = "MISS";
            if (hit) {
                defender.ships.get(shipIdx).remove(coordIdx); // rimuovi il pezzo colpito
                if (defender.ships.get(shipIdx).isEmpty()) {
                    defender.ships.remove(shipIdx); // nave affondata
                    result = "SUNK";
                } else {
                    result = "HIT";
                }
            }

            this.out.println("ATTACK_RESULT:" + x + "," + y + "," + result);
            defender.out.println("INCOMING_ATTACK:" + x + "," + y + "," + result);

            if (defender.ships.isEmpty()) {
                broadcast("GAME_OVER:" + this.name);
            } else {
                this.myTurn = false;
                defender.myTurn = true;
                this.out.println("TURN_CHANGE:false");
                defender.out.println("TURN_CHANGE:true");
            }
        }
    }

    private static void checkGameStart() {
        if (players.size() == 2 && players.get(0).ready && players.get(1).ready) {
            players.get(0).myTurn = true;
            players.get(0).out.println("GAME_START:true");
            players.get(1).out.println("GAME_START:false");
        }
    }

    private static void broadcast(String msg) {
        for (PlayerHandler p : players) p.out.println(msg);
    }
}