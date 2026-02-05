import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    private static char[][] myBoard = new char[10][10];
    private static char[][] enemyBoard = new char[10][10];
    private static boolean myTurn = false;
    private static String statusMessage = "Connessione in corso...";
    private static PrintWriter out;
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws IOException {
        for (char[] row : myBoard) Arrays.fill(row, '~');
        for (char[] row : enemyBoard) Arrays.fill(row, '~');

        Socket socket = new Socket("localhost", 3000);
        out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        System.out.print("Inserisci il tuo nome: ");
        String name = scanner.nextLine();
        out.println("JOIN:" + name);

        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    handleServerMessage(msg);
                }
            } catch (IOException e) {
                statusMessage = "Errore di connessione!";
                printBoards();
            }
        }).start();
    }

    private static void handleServerMessage(String msg) {
        if (msg.startsWith("MSG:")) {
            statusMessage = msg.substring(4);
        } else if (msg.equals("JOIN_OK")) {
            placeShips();
        } else if (msg.startsWith("GAME_START:")) {
            myTurn = msg.split(":")[1].equals("true");
            statusMessage = myTurn ? "GIOCO INIZIATO! Tocca a te." : "GIOCO INIZIATO! Attendi l'avversario.";
            printBoards();
            if (myTurn) attack();
        } else if (msg.startsWith("ATTACK_RESULT:")) {
            String[] p = msg.split(":")[1].split(",");
            char res = p[2].equals("MISS") ? 'O' : 'X';
            enemyBoard[Integer.parseInt(p[1])][Integer.parseInt(p[0])] = res;
            statusMessage = "Hai sparato in (" + p[0] + "," + p[1] + "): " + p[2];
            printBoards();
        } else if (msg.startsWith("INCOMING_ATTACK:")) {
            String[] p = msg.split(":")[1].split(",");
            char res = p[2].equals("MISS") ? 'O' : 'X';
            myBoard[Integer.parseInt(p[1])][Integer.parseInt(p[0])] = res;
            statusMessage = "Il nemico ha sparato in (" + p[0] + "," + p[1] + "): " + p[2];
            printBoards();
        } else if (msg.startsWith("TURN_CHANGE:")) {
            myTurn = msg.split(":")[1].equals("true");
            if (myTurn) {
                statusMessage = "È il tuo turno! Attacca!";
                printBoards();
                attack();
            }
        } else if (msg.startsWith("GAME_OVER:")) {
            statusMessage = "FINE PARTITA! Vincitore: " + msg.split(":")[1];
            printBoards();
            System.exit(0);
        }
    }

    private static void placeShips() {
        int[] sizes = {3, 2, 1};
        StringBuilder sb = new StringBuilder("PLACE_SHIPS:");
        
        for (int i = 0; i < sizes.length; i++) {
            statusMessage = "Posiziona nave di taglia " + sizes[i];
            printBoards();
            System.out.println("Inserisci coordinate X e Y separate da spazio:");
            int x = scanner.nextInt();
            int y = scanner.nextInt();
            
            for (int j = 0; j < sizes[i]; j++) {
                myBoard[y][x + j] = 'S';
                sb.append((x + j)).append(",").append(y).append(j == sizes[i] - 1 ? "" : ";");
            }
            if (i < sizes.length - 1) sb.append("|");
        }
        out.println(sb.toString());
    }

    private static void attack() {
        System.out.println("Coordinate attacco (x y):");
        int x = scanner.nextInt();
        int y = scanner.nextInt();
        out.println("ATTACK:" + x + "," + y);
    }

    private static void printBoards() {
        // pulisce lo schermo
        System.out.print("\033[H\033[2J");
        System.out.flush();
        
        System.out.println("==============================================");
        System.out.println(" STATO: " + statusMessage);
        System.out.println("==============================================");
        System.out.println("   TUO CAMPO                CAMPO NEMICO");
        System.out.println("   0 1 2 3 4 5 6 7 8 9      0 1 2 3 4 5 6 7 8 9");
        
        for (int i = 0; i < 10; i++) {
            System.out.print(i + " ");
            for (char c : myBoard[i]) System.out.print(c + " ");
            System.out.print("   " + i + " ");
            for (char c : enemyBoard[i]) System.out.print(c + " ");
            System.out.println();
        }
        System.out.println("==============================================");
    }
}