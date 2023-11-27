package byow.Core;

import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;
import edu.princeton.cs.algs4.StdDraw;

import static byow.Core.Engine.Constants.*;
import static byow.Core.RandomUtils.*;

import java.awt.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class Engine {
    public static final int WIDTH = 80;
    public static final int HEIGHT = 30;
    private final int margin = 1;
    private TETile[][] tiles;
    private Random random;
    private Agent agent = new Agent(0, 0);
    private int points = 0;

    public static class Constants {
        public static final int LIGHT_RANGE = 5;
    }

    /*
    define the agent by its coordinates
     */
    private static class Agent {
        private int x;
        private int y;

        public Agent(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public double distance(int i, int j) {
            return Math.abs(i - this.x) + Math.abs(j - this.y);
        }
    }

    private static class Room {
        private int tlx;
        private int tly;
        private int brx;
        private int bry;
        private int width;
        private int height;

        public Room(int x, int y) {
            this.tlx = x;
            this.tly = y;
        }
    }

    /**
     * Method used for exploring a fresh world. This method should handle all inputs,
     * including inputs from the main menu.
     */
    public void interactWithKeyboard() {

        boolean newGame;
        long seed = 0;
        while (true) {
            menu();
            if (StdDraw.hasNextKeyTyped()) {
                StdDraw.clear();
                char c = Character.toUpperCase(StdDraw.nextKeyTyped());
                if (c == 'N') {
                    StdDraw.text(0.5 * WIDTH, 0.5 * HEIGHT, "Please input the seed: ");
                    seed = getSeed();
                    newGame = true;
                    break;
                }
                if (c == 'L') {
                    tiles = readTiles();
                    findAgent();
                    newGame = false;
                    break;
                }
                if (c == 'Q') {
                    return;
                }
            }
        }
        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH, HEIGHT + 2, 0, 2);
        if (newGame) {
            random = new Random(seed);
            initialize();
            generateRandomRooms(seed, WIDTH - margin, HEIGHT - margin);
            generateAgent(seed);
            ter.renderFrame(tiles);
            moveWithKeyboard(ter, seed);
            saveArray(tiles);
        }
        if (!newGame) {
            ter.renderFrame(tiles);
            moveWithKeyboard(ter, seed);
            saveArray(tiles);
        }
    }

    private void showTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        Date date = new Date();
        StdDraw.setPenColor(Color.white);
        StdDraw.textLeft(1, HEIGHT, dateFormat.format(date));
        StdDraw.text(WIDTH * 3 / 4, HEIGHT, "Press O to turn on/off the light");
        StdDraw.text(WIDTH - 8, HEIGHT, "Points: " + (points));
        StdDraw.show();
    }

    private void moveWithKeyboard(TERenderer ter, long seed) {
        boolean lightsOff = false;
        while (true) {
            showTime();
            if (StdDraw.hasNextKeyTyped()) {
                char c = Character.toUpperCase(StdDraw.nextKeyTyped());
                if (c == 'O') {
                    if (!lightsOff) {
                        lightsOff = true;
                        TETile[][] darkTiles = TETile.copyOf(tiles);
                        turnOffLights(darkTiles);
                        ter.renderFrame(darkTiles);
                    } else {
                        lightsOff = false;
                        ter.renderFrame(tiles);
                    }
                }
                if (c == 'G') {
                    generateBeans(seed);
                }
                if (c == ':') {
                    while (true) {
                        if (StdDraw.hasNextKeyTyped()) {
                            char q = Character.toUpperCase(StdDraw.nextKeyTyped());
                            if (q == 'Q') {
                                return;
                            } else {
                                break;
                            }
                        }
                    }
                } else if (validMove(c)) {
                    makeOneMove(c);
                    if (lightsOff) {
                        TETile[][] darkTiles = TETile.copyOf(tiles);
                        turnOffLights(darkTiles);
                        ter.renderFrame(darkTiles);
                    } else {
                        ter.renderFrame(tiles);
                    }
                }
            }
        }
    }

    private void turnOffLights(TETile[][] darkTiles) {
        int numXTiles = darkTiles.length;
        int numYTiles = darkTiles[0].length;
        for (int x = 0; x < numXTiles; x += 1) {
            for (int y = 0; y < numYTiles; y += 1) {
                if (agent.distance(x, y) > LIGHT_RANGE) {
                    darkTiles[x][y] = Tileset.NOTHING;
                }
            }
        }
    }

    private long getSeed() {
        String input = "";
        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char c = Character.toUpperCase(StdDraw.nextKeyTyped());
                if (c == 'S' || c == 's') {
                    break;
                }
                if (Character.isDigit(c)) {
                    input += c;
                    StdDraw.clear();
                    StdDraw.text(0.5 * WIDTH, 0.5 * 6 / 5 * HEIGHT, "Please input the seed: ");
                    StdDraw.text(0.5 * WIDTH, 0.5 * HEIGHT, "(End with 'S')");
                    StdDraw.text(0.5 * WIDTH, 0.5 * 4 / 5 * HEIGHT, input);
                }
            }
        }
        return (long) Math.abs(Math.pow(2, 4 * 4 * 2) - Long.parseLong(input));
    }

    private void menu() {
        Font font = new Font("Consolas", Font.PLAIN, 2 * 3 * 5);
        StdDraw.setFont(font);
        StdDraw.setXscale(0, WIDTH);
        StdDraw.setYscale(0, HEIGHT);
        StdDraw.setPenColor(StdDraw.BLACK);
        StdDraw.text(0.5 * WIDTH, 0.5 * 6 / 5 * HEIGHT, "New World (N)");
        StdDraw.text(0.5 * WIDTH, 0.5 * HEIGHT, "Load (L)");
        StdDraw.text(0.5 * WIDTH, 0.5 * 4 / 5 * HEIGHT, "Quit (Q)");
    }

    /**
     * Method used for auto-grading and testing your code. The input string will be a series
     * of characters (for example, "n123sswwdasdassadwas", "n123sss:q", "lwww"). The engine should
     * behave exactly as if the user typed these characters into the engine using
     * interactWithKeyboard.
     * <p>
     * Recall that strings ending in ":q" should cause the game to quite save. For example,
     * if we do interactWithInputString("n123sss:q"), we expect the game to run the first
     * 7 commands (n123sss) and then quit and save. If we then do
     * interactWithInputString("l"), we should be back in the exact same state.
     * <p>
     * In other words, running both of these:
     * - interactWithInputString("n123sss:q")
     * - interactWithInputString("lww")
     * <p>
     * should yield the exact same world state as:
     * - interactWithInputString("n123sssww")
     *
     * @param input the input string to feed to your program
     */
    public TETile[][] interactWithInputString(String input) {
        // passed in as an argument, and return a 2D tile representation of the
        // world that would have been drawn if the same inputs had been given
        // to interactWithKeyboard().
        //
        // See proj3.byow.InputDemo for a demo of how you can make a nice clean interface
        // that works for many different input types.
        input = input.toUpperCase();
        boolean save = saveAfterQuit(input);

        if (input.charAt(0) == 'L') {
            tiles = readTiles();
            findAgent();
            String movements = extractMovements(input);
            TERenderer ter = new TERenderer();
            ter.initialize(WIDTH, HEIGHT + 2, 0, 2);
            ter.renderFrame(tiles);
            move(movements, ter);

        } else {
            long seed = (long) Math.abs(Math.pow(2, 4 * 4 * 2) - Long.parseLong(extractSeed(input)));
            String movements = extractMovements(input);
            random = new Random(seed);
            initialize();
            generateRandomRooms(seed, WIDTH - margin, HEIGHT - margin);
            generateAgent(seed);
            TERenderer ter = new TERenderer();
            ter.initialize(WIDTH, HEIGHT + 2, 0, 2);
            ter.renderFrame(tiles);
            move(movements, ter);
        }
        if (save) {
            saveArray(tiles);
        }
        return tiles;
    }

    private void findAgent() {
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                if (tiles[i][j] == Tileset.AVATAR) {
                    agent = new Agent(i, j);
                }
            }
        }
    }

    private void move(String movements, TERenderer ter) {
        StdDraw.pause((int) Math.pow(2, 2 * 5));
        int n = movements.length();
        for (int i = 0; i < n; i++) {
            char c = movements.charAt(i);
            if (validMove(c)) {
                makeOneMove(c);
                ter.renderFrame(tiles);
                StdDraw.pause((int) Math.pow(2, 2 * 4));
            }
        }
    }

    private boolean validMove(char c) {
        if (c == 'W') {
            if (tiles[agent.x][agent.y + 1] == Tileset.DOT) {
                points++;
            }
            return tiles[agent.x][agent.y + 1] != Tileset.WALL;
        } else if (c == 'S') {
            if (tiles[agent.x][agent.y - 1] == Tileset.DOT) {
                points++;
            }
            return tiles[agent.x][agent.y - 1] != Tileset.WALL;
        } else if (c == 'A') {
            if (tiles[agent.x - 1][agent.y] == Tileset.DOT) {
                points++;
            }
            return tiles[agent.x - 1][agent.y] != Tileset.WALL;
        } else if (c == 'D') {
            if (tiles[agent.x + 1][agent.y] == Tileset.DOT) {
                points++;
            }
            return tiles[agent.x + 1][agent.y] != Tileset.WALL;
        }
        return false;
    }

    private void makeOneMove(char c) {
        if (tiles[agent.x][agent.y] == Tileset.DOT) {
            points += 1;
        }
        tiles[agent.x][agent.y] = Tileset.FLOOR;
        if (c == 'W') {
            tiles[agent.x][++agent.y] = Tileset.AVATAR;
        } else if (c == 'S') {
            tiles[agent.x][--agent.y] = Tileset.AVATAR;
        } else if (c == 'A') {
            tiles[--agent.x][agent.y] = Tileset.AVATAR;
        } else if (c == 'D') {
            tiles[++agent.x][agent.y] = Tileset.AVATAR;
        }
    }

    private void generateAgent(long seed) {
        boolean flag = true;
        while (flag) {
            int x = (int) (WIDTH * uniform(new Random(seed)));
            int y = (int) (HEIGHT * uniform(new Random(seed)));
            if (tiles[x][y] == Tileset.FLOOR) {
                flag = false;
                agent.x = x;
                agent.y = y;
                tiles[x][y] = Tileset.AVATAR;
            } else {
                seed *= 2;
            }
        }
    }

    private void generateBeans(long seed) {
        for (int i = 0; i < 2 * 5; i++) {
            boolean flag = true;
            while (flag) {
                Random ran = new Random(seed * (long) Math.pow(2, i));
                int x = (int) (WIDTH * uniform(ran));
                int y = (int) (HEIGHT * uniform(ran));
                if (tiles[x][y] == Tileset.FLOOR) {
                    flag = false;
                    tiles[x][y] = Tileset.DOT;
                } else {
                    seed *= 3;
                }
            }
        }

    }

    private boolean saveAfterQuit(String input) {
        return input.toUpperCase().substring(input.length() - 2).equals(":Q");
    }

    private String extractSeed(String input) {
        if (input.charAt(0) == 'N') {
            return input.substring(1, input.indexOf('S'));
        }
        return "";
    }

    private String extractMovements(String input) {
        if (saveAfterQuit(input)) {
            input = input.substring(0, input.length() - 2);
        }
        if (input.charAt(0) == 'N') {
            input = input.substring(input.indexOf('S') + 1);
        } else if (input.charAt(0) == 'L') {
            input = input.substring(1);
        }
        return input;
    }

    private boolean inBound(int x, int y) {
        return (x > 0 && x < WIDTH - margin && y > margin && y < HEIGHT - margin);
    }

    private void initialize() {
        tiles = new TETile[WIDTH][HEIGHT];
        for (int x = 0; x < WIDTH; x++) {
            Arrays.fill(tiles[x], Tileset.NOTHING);
        }
    }

    private void wall(int x, int y) {
        if (tiles[x][y] != Tileset.FLOOR) {
            tiles[x][y] = Tileset.WALL;
        }
    }

    private void turn(Room curr, Room next) {
        int width = uniform(random, 1, 3);
        int x = uniform(random, curr.tlx, curr.brx - 1);
        int y = uniform(random, next.bry, next.tly - 1);
        for (int i = x; i < next.tlx; i++) {
            tiles[i][y] = Tileset.FLOOR;
            wall(i, y - 1);
            if (width == 1) {
                wall(i, y + 1);
            } else {
                tiles[i][y + 1] = Tileset.FLOOR;
                wall(i, y + 2);
            }
        }
        int startY = 0;
        int endY = 0;
        if (curr.bry > next.tly) {
            startY = y;
            endY = curr.bry;
        } else if (next.bry > curr.tly) {
            startY = curr.tly + 1;
            endY = y + 1;
        }
        for (int i = startY; i < endY; i++) {
            tiles[x][i] = Tileset.FLOOR;
            wall(x - 1, i);
            if (width == 1) {
                wall(x + 1, i);
            } else {
                tiles[x + 1][i] = Tileset.FLOOR;
                wall(x + 2, i);
            }
        }
    }

    private void edgeCase(Room curr, Room next) {
        if (curr.brx == next.tlx || next.brx == curr.tlx) {
            if (curr.brx == next.tlx && curr.bry > next.tly + 1) {
                for (int i = next.tly + 1; i < curr.bry; i++) {
                    tiles[curr.brx][i] = Tileset.FLOOR;
                    wall(curr.brx - 1, i);
                    wall(curr.brx + 1, i);
                }
            } else if (next.brx == curr.tlx && next.bry > curr.tly + 1) {
                for (int i = curr.tly + 1; i < next.bry; i++) {
                    tiles[curr.tlx][i] = Tileset.FLOOR;
                    wall(curr.tlx - 1, i);
                    wall(curr.tlx + 1, i);
                }
            }
        } else if (curr.bry == next.tly || next.bry == curr.tly) {
            if (curr.bry == next.tly && curr.brx + 1 < next.tlx) {
                for (int i = curr.brx + 1; i < next.tlx; i++) {
                    tiles[i][curr.bry] = Tileset.FLOOR;
                    wall(i, curr.bry + 1);
                    wall(i, curr.bry - 1);
                }
            } else if (next.bry == curr.tly && next.brx + 1 < curr.tlx) {
                for (int i = next.brx + 1; i < curr.tlx; i++) {
                    tiles[i][next.bry] = Tileset.FLOOR;
                    wall(i, next.bry + 1);
                    wall(i, next.bry - 1);
                }
            }
        }
    }

    private void edgeCaseY(Room curr, Room next, int width) {
        if (curr.bry + 1 < next.tly) {
            int y = uniform(random, curr.bry, next.tly);
            for (int i = curr.brx + 1; i < next.tlx; i++) {
                tiles[i][y] = Tileset.FLOOR;
                wall(i, y - 1);
                if (width == 1) {
                    wall(i, y + 1);
                } else {
                    tiles[i][y + 1] = Tileset.FLOOR;
                    wall(i, y + 2);
                }
            }
        } else if (curr.bry + 1 == next.tly) {
            for (int i = curr.brx + 1; i < next.tlx; i++) {
                if (width == 1) {
                    int y = uniform(random, curr.bry, next.tly + 1);
                    tiles[i][y] = Tileset.FLOOR;
                    wall(i, y - 1);
                    wall(i, y + 1);
                } else {
                    int y = curr.bry;
                    tiles[i][y] = Tileset.FLOOR;
                    tiles[i][y + 1] = Tileset.FLOOR;
                    wall(i, y - 1);
                    wall(i, y + 2);
                }
            }
        }
    }

    private void edgeCaseX(Room curr, Room next, int width) {
        if (curr.brx > next.tlx + 1) {
            int x = uniform(random, next.tlx, curr.brx);
            for (int i = curr.tly + 1; i < next.bry; i++) {
                tiles[x][i] = Tileset.FLOOR;
                wall(x - 1, i);
                if (width == 1) {
                    wall(x + 1, i);
                } else {
                    wall(x + 2, i);
                    tiles[x + 1][i] = Tileset.FLOOR;
                }
            }
        } else if (curr.brx == next.tlx + 1) {
            for (int i = curr.tly + 1; i < next.bry; i++) {
                if (width == 1) {
                    int x = uniform(random, next.tlx, curr.brx + 1);
                    tiles[x][i] = Tileset.FLOOR;
                    wall(x - 1, i);
                    wall(x + 1, i);
                } else {
                    int x = next.tlx;
                    tiles[x][i] = Tileset.FLOOR;
                    wall(x - 1, i);
                    wall(x + 2, i);
                    tiles[x + 1][i] = Tileset.FLOOR;
                }
            }
        }
    }

    private void straight(Room curr, Room next) {
        edgeCase(curr, next);
        int width = uniform(random, 1, 3);
        if (curr.bry + 1 <= next.tly) {
            edgeCaseY(curr, next, width);
        } else if (next.bry + 1 <= curr.tly) {
            edgeCaseY(next, curr, width);
        } else if (curr.brx >= next.tlx + 1) {
            edgeCaseX(curr, next, width);
        } else if (next.brx >= curr.tlx + 1) {
            edgeCaseX(next, curr, width);
        }
    }

    private void generateHallways(List<Room> rooms, int roomNum) {
        for (int i = 0; i < roomNum - 1; i++) {
            Room curr = rooms.get(i);
            Room next = rooms.get(i + 1);
            if (next.brx < curr.tlx && (next.bry > curr.tly || next.tly < curr.bry)) {
                turn(next, curr);
            } else if (curr.brx < next.tlx && (curr.bry > next.tly || curr.tly < next.bry)) {
                turn(curr, next);
            } else {
                straight(curr, next);
            }
        }
    }

    private void generateRandomRooms(Long seed, int width, int height) {
        Font font = new Font("Monaco", Font.PLAIN, 2 * 8);
        StdDraw.setFont(font);
        // int roomNum = (int) gaussian(random, uniform(random, 12, 25), uniform(random, 0, 10));
        int upper = 2 * 2 * 2 * 3 + 1;
        int lower = 2 * 2 * 3;
        int roomNum = uniform(random, lower, upper);
        List<Integer> xs = new ArrayList<>();
        List<Integer> ys = new ArrayList<>();
        for (int i = 1; i <= roomNum; i++) {
            xs.add((int) (width * uniform(new Random(seed * i))));
            ys.add((int) (height * uniform(new Random(seed * 2 * i))));
        }

        List<Room> rooms = new ArrayList<>();
        // initiate rooms by defining their top-left coordinates
        for (int i = 0; i < roomNum; i++) {
            rooms.add(new Room(xs.get(i), ys.get(i)));
        }
        // expand points into rooms
        Iterator<Room> it = rooms.listIterator();
        while (it.hasNext()) {
            Room room = it.next();
            adjustPosition(room, width, height);
            boolean flag = false;
            int n = 1;
            // randomly generate the width and height
            while (!flag && n < 2 * 2 * 2) {
                room.width = uniform(new Random((long) room.tlx * n), 2, 8);
                room.height = uniform(new Random((long) room.tly * 2 * n), 2, 8);
                n++;
                if (inBound(room.tlx + room.width, room.tly - room.height)) {
                    flag = true;
                    room.brx = room.tlx + room.width;
                    room.bry = room.tly - room.height;
                }
            }
            // Erase the point, if no right size was made.
            if (!flag) {
                tiles[room.tlx][room.tly] = Tileset.NOTHING;
                it.remove();
                roomNum -= 1;
            } else {
                // draw rooms
                for (int i = room.tlx; i <= room.brx; i++) {
                    for (int j = room.bry; j <= room.tly; j++) {
                        tiles[i][j] = Tileset.FLOOR;
                    }
                }
            }
        }
        generateHallwaysTwo(rooms, roomNum);
        buildWalls();
        generateBeans(seed);
    }

    private void adjustPosition(Room room, int width, int height) {
        // if the point is far too down, move it up a bit
        if (room.tly < margin + 2) {
            tiles[room.tlx][room.tly] = Tileset.NOTHING;
            room.tly += 5;
        }
        // if the point is far too down, move it up a bit
        if (room.tly > height - margin - 2) {
            tiles[room.tlx][room.tly] = Tileset.NOTHING;
            room.tly -= 3;
        }
        // if the point is far too right, move it left a bit
        if (room.tlx > width - margin - 2) {
            tiles[room.tlx][room.tly] = Tileset.NOTHING;
            room.tlx -= 5;
        }
        // if the point is far too left, move it right a bit
        if (room.tlx < margin + 2) {
            tiles[room.tlx][room.tly] = Tileset.NOTHING;
            room.tlx += 5;
        }
    }

    private boolean isEdge(int x, int y) {
        boolean flag = false;
        List<Integer> xs = Arrays.asList(-1, -1, -1, 0, 0, 1, 1, 1);
        List<Integer> ys = Arrays.asList(1, 0, -1, 1, -1, 1, 0, -1);
        for (int i : xs) {
            for (int j : ys) {
                if (tiles[x + i][y + j] != Tileset.FLOOR) {
                    flag = true;
                    break;
                }
            }
        }
        return flag;
    }

    private void buildWall(int x, int y) {
        List<Integer> xs = Arrays.asList(-1, -1, -1, 0, 0, 1, 1, 1);
        List<Integer> ys = Arrays.asList(1, 0, -1, 1, -1, 1, 0, -1);
        for (int i : xs) {
            for (int j : ys) {
                if (tiles[x + i][y + j] == Tileset.NOTHING) {
                    tiles[x + i][y + j] = Tileset.WALL;
                }
            }
        }
    }

    private void buildWalls() {
        for (int x = 1; x < WIDTH - 1; x++) {
            for (int y = 1; y < HEIGHT - 1; y++) {
                if (tiles[x][y] == Tileset.FLOOR && isEdge(x, y)) {
                    buildWall(x, y);
                }
            }
        }
    }

    public String toString() {
        int width = tiles.length;
        int height = tiles[0].length;
        StringBuilder sb = new StringBuilder();

        for (int y = height - 1; y >= 0; y -= 1) {
            for (int x = 0; x < width; x += 1) {
                if (tiles[x][y] == null) {
                    throw new IllegalArgumentException("Tile at position x=" + x + ", y=" + y + " is null.");
                }
                sb.append(tiles[x][y].character());
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public static void saveArray(TETile[][] array) {
        FileWriter writeFile = null;
        try {
            File file = new File("Tiles.txt");
            if (!file.exists()) {
                file.createNewFile();
            }
            writeFile = new FileWriter(file);
            for (TETile[] teTiles : array) {
                for (int j = 0; j < array[0].length - 1; j++) {
                    writeFile.write(teTiles[j].character() + ",");
                }
                writeFile.write(teTiles[array[0].length - 1].character());
                writeFile.write("\n");
            }
            writeFile.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writeFile != null) {
                    writeFile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static TETile[][] readTiles() {
        FileReader reader = null;
        BufferedReader readerBuf = null;
        TETile[][] array = null;
        try {
            reader = new FileReader("Tiles.txt");
            readerBuf = new BufferedReader(reader);
            List<String> strList = new ArrayList<>();
            String lineStr;
            while ((lineStr = readerBuf.readLine()) != null) {
                strList.add(lineStr);
            }
            String s = strList.get(0);
            int columnNum = s.split("\\,").length;
            array = new TETile[strList.size()][columnNum];
            int count = 0;
            for (String str : strList) {
                String[] strings = str.split("\\,");
                for (int i = 0; i < columnNum; i++) {
                    array[count][i] = identifyTile(strings[i]);
                }
                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (readerBuf != null) {
                    readerBuf.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return array;
    }

    private static TETile identifyTile(String str) {
        return switch (str) {
            case " " -> Tileset.NOTHING;
            case "·" -> Tileset.FLOOR;
            case "@" -> Tileset.AVATAR;
            case "#" -> Tileset.WALL;
            case "⬤" -> Tileset.DOT;
            default -> null;
        };
    }

    private void generateHallwaysTwo(List<Room> rooms, int roomNum) {

        for (int i = 0; i < roomNum - 1; i++) {
            Room room1 = rooms.get(i);
            Room room2 = rooms.get(i + 1);

            int[] room1EdgePoint = getRandomEdgePoint(room1);
            int[] room2EdgePoint = getRandomEdgePoint(room2);

            int x1 = room1EdgePoint[0];
            int y1 = room1EdgePoint[1];
            int x2 = room2EdgePoint[0];
            int y2 = room2EdgePoint[1];

            while (x1 != x2 || y1 != y2) {
                int deltaX = x2 - x1;
                int deltaY = y2 - y1;

                boolean moveHorizontal = random.nextBoolean();
                boolean canMoveHorizontal = deltaX != 0;
                boolean canMoveVertical = deltaY != 0;

                if (canMoveHorizontal && (moveHorizontal || !canMoveVertical)) {
                    x1 += deltaX > 0 ? 1 : -1;
                } else if (canMoveVertical) {
                    y1 += deltaY > 0 ? 1 : -1;
                }

                tiles[x1][y1] = Tileset.FLOOR;
            }
        }
    }

    private int[] getRandomEdgePoint(Room room) {
        int edge = random.nextInt(4); // Choose a random edge (0: top, 1: right, 2: bottom, 3: left)
        int x, y;

        switch (edge) {
            case 0:
                x = room.tlx + random.nextInt(room.width);
                y = room.tly;
                break;
            case 1:
                x = room.brx;
                y = room.tly - random.nextInt(room.height);
                break;
            case 2:
                x = room.tlx + random.nextInt(room.width);
                y = room.bry;
                break;
            default:
                x = room.tlx;
                y = room.tly - random.nextInt(room.height);
                break;
        }
        return new int[]{x, y};
    }
}