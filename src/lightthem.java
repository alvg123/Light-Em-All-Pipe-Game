import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import java.util.Random;
import java.util.LinkedList;
import java.util.Map;

import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;


/*User guide:  

 * you have a powerstation, and need to 

 * reoatet the pipes so that the wires 

 * on each pipe connect to each other

 * You win the game when all the titles/gamepieces 

 * on the board all connect to each other
 
 */

//what stays the same in the lightemworld
interface WorldConstants {
  int CELL_SIZE = 20;
}

//represents two gamepieces connected (edge)
class Edge {
  GamePiece fromNode;
  GamePiece toNode;
  int weight;

  // constructor for edge
  public Edge(GamePiece fromNode, GamePiece toNode) {
    this.fromNode = fromNode;
    this.toNode = toNode;
    this.weight = new Random().nextInt(100);
  }

}

//data structure that allows us to take gps and partition them into groups 
class UnionFind {
  public Map<GamePiece, GamePiece> parent = new HashMap<>();

  // adds the nodes to the parent (Hashmap)
  public void add(GamePiece node) {
    parent.put(node, node);
  }

  // puts the node with its parent
  public GamePiece find(GamePiece node) {
    if (parent.get(node) != node) {
      parent.put(node, find(parent.get(node)));
    }
    return parent.get(node);
  }

  //unites the parent with two different nodes
  public void union(GamePiece node1, GamePiece node2) {
    GamePiece root1 = find(node1);
    GamePiece root2 = find(node2);
    if (root1 != root2) {
      parent.put(root1, root2);
    }
  }
}

//game world class 
class LightEmAll extends World implements WorldConstants {
  // a list of columns of GamePieces,
  // i.e., represents the board in column-major order
  ArrayList<ArrayList<GamePiece>> board;
  // a list of all nodes
  ArrayList<GamePiece> nodes; // might be helpful instead of using a nested for loop
  // a list of edges of the minimum spanning tree
  ArrayList<Edge> mst;
  // the width and height of the board
  int width; // column
  int height; // row

  ArrayList<Edge> edges;

  // the current location of the power station,
  // as well as its effective radius
  int powerRow;
  int powerCol;
  int radius;
  boolean gameOver;

  // construtor for gameworld class
  LightEmAll(int width, int height, int powerRow, int powerCol, int radius, boolean gameOver) {
    this.width = width;
    this.height = height;
    this.powerRow = powerRow;
    this.powerCol = powerCol;
    this.radius = radius;
    this.gameOver = gameOver;
    this.board = new ArrayList<ArrayList<GamePiece>>();
    this.mst = new ArrayList<Edge>();
    this.nodes = new ArrayList<GamePiece>();
    this.edges = new ArrayList<Edge>();
    // Initialize additional fields as needed

    initBoard(); // Initialize the game board
    createAllEdges();
    placePs();
    generateMST();
    randomizeBoard();
    wiresConnectedToPsBfs();
  }

  // for testing
  LightEmAll(int width, int height, int powerRow, int powerCol, int radius, boolean gameOver,
      ArrayList<ArrayList<GamePiece>> board) {
    this.width = width;
    this.height = height;
    this.powerRow = powerRow;
    this.powerCol = powerCol;
    this.radius = radius;
    this.gameOver = gameOver;
    this.board = new ArrayList<ArrayList<GamePiece>>();
    this.mst = new ArrayList<Edge>();
    this.nodes = new ArrayList<GamePiece>();
    this.edges = new ArrayList<Edge>();
    // Initialize additional fields as needed

    initBoard(); // Initialize the game board
    createAllEdges();
    placePs();
    generateMST();
    randomizeBoard();
    wiresConnectedToPsBfs();
  }

  // rotates the tiles
  public void onMousePressed(Posn pos, String buttonName) {
    int row = pos.y / CELL_SIZE;
    int col = pos.x / CELL_SIZE;

    if (row >= 0 && row < height && col >= 0 && col < width) {
      GamePiece gp = board.get(row).get(col);
      if (buttonName.equals("LeftButton")) {
        gp.rotate();
      }
    }
    wiresConnectedToPsBfs();
  }

  // randomly rotates each gamepiece
  void randomizeBoard() {
    Random rand = new Random();
    for (ArrayList<GamePiece> row : this.board) {
      for (GamePiece piece : row) {
        int rotations = rand.nextInt(4);
        for (int i = 0; i < rotations; i++) {
          piece.rotate();
        }
      }
    }
  }

  // crates the board
  void initBoard() {
    this.board = new ArrayList<ArrayList<GamePiece>>();
    for (int i = 0; i < this.height; i++) {
      ArrayList<GamePiece> row = new ArrayList<>();
      for (int j = 0; j < this.width; j++) {
        GamePiece piece = new GamePiece(i, j);
        row.add(piece);
      }
      this.board.add(row);
    }
  }

  // places the powerstation at the top-left corner
  public void placePs() {
    this.board.get(0).get(0).nowHasPs();
    this.board.get(0).get(0).hasP();
  }

  // moves the powerState
  public void onKeyEvent(String key) {

    GamePiece currentPs = board.get(this.powerRow).get(this.powerCol);

    if (key.equals("up") && this.powerRow > 0
        && currentPs.arePiecesConneted(board.get(this.powerRow - 1).get(this.powerCol))) {
      board.get(this.powerRow - 1).get(this.powerCol).nowHasPs();
      this.powerRow = this.powerRow - 1;
      currentPs.notHasPs();
      // currentPs.unP();
    }

    if (key.equals("down") && this.powerRow < this.height - 1
        && currentPs.arePiecesConneted(board.get(this.powerRow + 1).get(this.powerCol))) {
      board.get(this.powerRow + 1).get(this.powerCol).nowHasPs();
      this.powerRow = this.powerRow + 1;
      currentPs.notHasPs();
      // currentPs.unP();

    }

    if (key.equals("left") && this.powerCol > 0
        && currentPs.arePiecesConneted(board.get(this.powerRow).get(this.powerCol - 1))) {
      board.get(this.powerRow).get(this.powerCol - 1).nowHasPs();
      this.powerCol = this.powerCol - 1;
      currentPs.notHasPs();
      // currentPs.unP();
    }

    if (key.equals("right") && this.powerCol < this.width - 1
        && currentPs.arePiecesConneted(board.get(this.powerRow).get(this.powerCol + 1))) {
      board.get(this.powerRow).get(this.powerCol + 1).nowHasPs();
      this.powerCol = this.powerCol + 1;
      currentPs.notHasPs();
      // currentPs.unP();
    }

    wiresConnectedToPsBfs();

  }

  // checks if all the cells are connected to powerStatation->gameover
  void wiresConnectedToPsBfs() {

    Queue<GamePiece> cellsToVisit = new LinkedList<GamePiece>(); // worklist
    Queue<GamePiece> alreadySeen = new LinkedList<GamePiece>();

    cellsToVisit.add(this.board.get(this.powerRow).get(this.powerCol));

    while (!cellsToVisit.isEmpty()) {
      GamePiece next = cellsToVisit.remove();

      if (alreadySeen.contains(next)) { // do nothing

      }

      else {
        // checks top and gets top

        if (next.row > 0 && next.arePiecesConneted(board.get(next.row - 1).get(next.col))) {
          cellsToVisit.add(board.get(next.row - 1).get(next.col));
          // add it cellsToVisit and aleadySeen
        }

        // checks left and gets left
        if (next.col > 0 && next.arePiecesConneted(board.get(next.row).get(next.col - 1))) {
          cellsToVisit.add(board.get(next.row).get(next.col - 1));
        }

        // checks bottom and gets bottom
        if (next.row < height - 1
            && next.arePiecesConneted(board.get(next.row + 1).get(next.col))) {
          cellsToVisit.add(board.get(next.row + 1).get(next.col));
        }

        // checks right and gets right
        if (next.col < width - 1 && next.arePiecesConneted(board.get(next.row).get(next.col + 1))) {
          cellsToVisit.add(board.get(next.row).get(next.col + 1));
        }

        // next.hasP();
        alreadySeen.add(next);

      }

    }

    for (ArrayList<GamePiece> row : this.board) {
      for (GamePiece gp : row) {
        if (alreadySeen.contains(gp)) {
          gp.hasP();
        }
        else {
          gp.unP();
        }
      }

    }

  }

  // makes the visual scene of the game
  public WorldScene makeScene() {
    WorldScene scene = getEmptyScene();
    int cellSize = WorldConstants.CELL_SIZE;
    for (int row = 0; row < height; row++) {
      for (int col = 0; col < width; col++) {
        GamePiece gp = board.get(row).get(col);
        Color wireColor = gp.powered ? Color.YELLOW : Color.GRAY;
        boolean hasPowerStation = gp.powerStation;
        WorldImage tileImage = gp.tileImage(cellSize, cellSize / 5, wireColor, hasPowerStation);
        scene.placeImageXY(tileImage, col * cellSize + cellSize / 2, row * cellSize + cellSize / 2);
      }
    }

    
    boolean gameOver = checkGameOver();
    if (gameOver) {
      WorldImage winningMessage = new TextImage("You Win!", 30, Color.GREEN);
      scene.placeImageXY(winningMessage, scene.width / 2, scene.height / 2);
    }
    return scene;
  }

  // checks if the game is over by seeing if the gps are all connected
  boolean checkGameOver() {
    for (ArrayList<GamePiece> row : board) {
      for (GamePiece gp : row) {
        if (!gp.powered) {
          return false;
        }
      }
    }
    return true;
  }

  // makes world to an end if the conditions are met
  public WorldEnd worldEnds() {
    boolean gameOver = checkGameOver();
    if (gameOver) {
      return new WorldEnd(true, makeWinningScene());
    }
    else {
      return new WorldEnd(false, makeScene());
    }
  }

  // tells the player they won
  WorldScene makeWinningScene() {
    WorldScene winningScene = getEmptyScene();
    WorldImage winningMessage = new TextImage("You Win!", 30, Color.GREEN);
    winningScene.placeImageXY(winningMessage, winningScene.width / 2, winningScene.height / 2);
    return winningScene;
  }

  // creates all the edges
  public void createAllEdges() {
    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        GamePiece current = board.get(i).get(j);
        if (j < width - 1) {
          edges.add(new Edge(current, board.get(i).get(j + 1)));
        }
        if (i < height - 1) {
          edges.add(new Edge(current, board.get(i + 1).get(j)));
        }
      }
    }

    edges.sort((e1, e2) -> e1.weight - e2.weight);
  }

  // makes an MST to make sure the gps/world are solvable
  public void generateMST() {
    UnionFind uf = new UnionFind();

    for (ArrayList<GamePiece> row : this.board) {
      for (GamePiece piece : row) {
        uf.add(piece);
      }
    }
    for (Edge edge : edges) {
      if (uf.find(edge.fromNode) != uf.find(edge.toNode)) {
        uf.union(edge.fromNode, edge.toNode);
        this.mst.add(edge);

      }
    }
    
    for (Edge edge : this.mst) {
      GamePiece from = edge.fromNode;
      GamePiece to = edge.toNode;
      // checks top and bottom
      if (from.sameCol(to)) {
        from.bottom = true;
        to.top = true;
      }

      else { // checks left and right
        from.left = true;
        to.right = true;
      }
    }

  }

}

//represents a cell/title in the game
class GamePiece {
  // in logical coordinates, with the origin
  // at the top-left corner of the screen
  int row;
  int col;
  // whether this GamePiece is connected to the
  // adjacent left, right, top, or bottom pieces
  boolean left;
  boolean right;
  boolean top;
  boolean bottom;
  // whether the power station is on this piece
  boolean powerStation;
  boolean powered;

  //constructor for gp
  GamePiece(boolean left, boolean right, boolean top, boolean bottom, boolean powerStation,
      boolean powered) {
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
  }

  // for generateMST()
  GamePiece(int row, int col) {
    this.row = row;
    this.col = col;
    this.left = false;
    this.right = false;
    this.top = false;
    this.bottom = false;
  }

  // for testing
  GamePiece(int row, int col, boolean powerStaion, boolean powered) {
    this.row = row;
    this.col = col;
    this.left = false;
    this.right = false;
    this.top = false;
    this.bottom = false;
    this.powerStation = false;
    this.powered = false;
  }

  // Generate an image of this, the given GamePiece.
  // - size: the size of the tile, in pixels
  // - wireWidth: the width of wires, in pixels
  // - wireColor: the Color to use for rendering wires on this
  // - hasPowerStation: if true, draws a fancy star on this tile to represent the
  // power station
  //

  // invole this method on each of gamepeice and placeAbove()place in stack and
  // place them along side each other and make a board.
  // Nested loop vcalled upon on this to make a board

  WorldImage tileImage(int size, int wireWidth, Color wireColor, boolean hasPowerStation) {
    // Start tile image off as a blue square with a wire-width square in the middle,
    // to make image "cleaner" (will look strange if tile has no wire, but that
    // can't be)
    WorldImage image = new OverlayImage(
        new RectangleImage(wireWidth, wireWidth, OutlineMode.SOLID, wireColor),
        new RectangleImage(size, size, OutlineMode.SOLID, Color.DARK_GRAY));
    WorldImage vWire = new RectangleImage(wireWidth, (size + 1) / 2, OutlineMode.SOLID, wireColor);
    WorldImage hWire = new RectangleImage((size + 1) / 2, wireWidth, OutlineMode.SOLID, wireColor);

    if (this.top) {
      image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, vWire, 0, 0, image);
    }
    if (this.right) {
      image = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE, hWire, 0, 0, image);
    }
    if (this.bottom) {
      image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM, vWire, 0, 0, image);
    }
    if (this.left) {
      image = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE, hWire, 0, 0, image);
    }
    if (hasPowerStation) {
      image = new OverlayImage(
          new OverlayImage(new StarImage(size / 3, 7, OutlineMode.OUTLINE, new Color(255, 128, 0)),
              new StarImage(size / 3, 7, OutlineMode.SOLID, new Color(0, 255, 255))),
          image);
    }
    return image;
  }

  // rotates the GamePiece when clicked clockwise
  public void rotate() {

    boolean currentTop = this.top;
    boolean currentRight = this.right;
    boolean currentBottom = this.bottom;
    boolean currentLeft = this.left;

    if (currentTop) {

      this.right = true;
    }

    else {
      this.right = false;
    }

    if (currentRight) {

      this.bottom = true;
    }

    else {
      this.bottom = false;
    }
    if (currentBottom) {

      this.left = true;
    }

    else {
      this.left = false;
    }

    if (currentLeft) {

      this.top = true;
    }

    else {
      this.top = false;
    }

  }

  // checks if this Gamepiece is connceted to each other.
  public boolean arePiecesConneted(GamePiece that) {

    // checks our leftpiece
    if (this.row == that.row && this.col == that.col + 1) {
      if (this.left && that.right) {
        return true;
      }
    }

    // checks our rightpiece
    if (this.row == that.row && this.col == that.col - 1) {
      if (this.right && that.left) {
        return true;
      }
    }

    // checks our toppiece
    if (this.row == that.row + 1 && this.col == that.col) {
      if (this.top && that.bottom) {
        return true;
      }
    }

    // checks our bottompiece
    if (this.row == that.row - 1 && this.col == that.col) {
      if (this.bottom && that.top) {
        return true;
      }
    }

    return false;

  }

  // places the powerstation on the gp
  public void nowHasPs() {
    this.powerStation = true;
  }

  // makes the gp not have a powerstation
  public void notHasPs() {
    this.powerStation = false;
  }

  // powers up the gp
  public void hasP() {
    this.powered = true;
  }

  // unpowers the gp
  public void unP() {
    this.powered = false;
  }

  // checks if this and that gp are in the same column
  boolean sameCol(GamePiece that) {
    return this.col == that.col;
  }

}

class Exampleslight {
  LightEmAll game1;
  LightEmAll game2;
  LightEmAll game3;

  GamePiece piece1;
  GamePiece piece2;
  GamePiece piece3;
  GamePiece piece4;
  GamePiece startingp;

  GamePiece samec;
  GamePiece samec2;
  GamePiece diffc;

  LightEmAll game0;

  void initData() {
    this.game1 = new LightEmAll(5, 5, 2, 2, 10, false);
    this.game2 = new LightEmAll(8, 8, 4, 4, 5, false);
    this.game3 = new LightEmAll(3, 3, 1, 1, 2, false);
    this.startingp = new GamePiece(0, 0);

    ArrayList<GamePiece> row1 = new ArrayList<>();
    row1.add(this.startingp);
    // row1.add(new Cell(false, false, false));
    ArrayList<ArrayList<GamePiece>> board1 = new ArrayList<>();
    // row2.add();
    // grid.add(row2)
    board1.add(row1);
    game0 = new LightEmAll(2, 2, 0, 0, 1, false, board1);

    // game0.board.add(startingp);

    this.piece1 = new GamePiece(true, false, true, false, false, false);
    this.piece2 = new GamePiece(false, true, false, true, false, false);
    this.piece3 = new GamePiece(true, true, true, true, true, true);
    this.piece4 = new GamePiece(false, false, false, false, false, false);

    this.samec = new GamePiece(4, 5);
    this.samec2 = new GamePiece(3, 5);
    this.diffc = new GamePiece(2, 2);

  }

  // testing if the pieces rotates
  void testRotatePiece(Tester t) {
    initData();
    GamePiece piece = game1.board.get(2).get(2);
    t.checkExpect(piece.top, false);
    t.checkExpect(piece.right, false);
    t.checkExpect(piece.bottom, false);
    t.checkExpect(piece.left, false);

    piece.rotate();
    t.checkExpect(piece.top, false);
    t.checkExpect(piece.right, false);
    t.checkExpect(piece.bottom, false);
    t.checkExpect(piece.left, false);
  }

  // testing to move the powerstatio n
  void testMovePowerStation(Tester t) {
    initData();
    game1.onKeyEvent("right");
    t.checkExpect(game1.powerCol, 2);
    game1.onKeyEvent("down");
    t.checkExpect(game1.powerRow, 2);
  }

  // testing intizating the board
  void testInitBoard(Tester t) {
    initData();
    t.checkExpect(game1.board.size(), 5);
    t.checkExpect(game1.board.get(0).size(), 5);
    t.checkExpect(game1.board.get(2).get(2).powerStation, false);
  }

  // testing if game is over/won
  void testGameOver(Tester t) {
    initData();
    for (ArrayList<GamePiece> row : game1.board) {
      for (GamePiece piece : row) {
        piece.powered = true;
      }
    }
    t.checkExpect(game1.checkGameOver(), true);
  }

  // testing if gps are connected
  void testArePiecesConnected(Tester t) {
    initData();
    t.checkExpect(piece1.arePiecesConneted(piece2), false);
    t.checkExpect(piece3.arePiecesConneted(piece4), false);
  }

  // testing if the gp has a powerstation or not
  void testNowHasPs(Tester t) {
    initData();
    t.checkExpect(piece1.powerStation, false);
    piece1.nowHasPs();
    t.checkExpect(piece1.powerStation, true);
  }

  // testing if the gp doesn't have powerstation
  void testNotHasPs(Tester t) {
    initData();
    piece3.notHasPs();
    t.checkExpect(piece3.powerStation, false);
  }

  // testing if the gp is powered
  void testHasP(Tester t) {
    initData();
    t.checkExpect(piece1.powered, false);
    piece1.hasP();
    t.checkExpect(piece1.powered, true);
  }

  // testing if the gp isn't powered
  void testUnp(Tester t) {
    initData();
    t.checkExpect(piece1.powered, false);
    piece1.hasP();
    t.checkExpect(piece1.powered, true);
    piece1.unP();
    t.checkExpect(piece1.powered, false);

  }

  // tests if this and that gp are in the same col or not
  boolean testsameCol(Tester t) {
    initData();
    return t.checkExpect(this.samec.sameCol(this.samec2), true)
        && t.checkExpect(this.samec.sameCol(this.diffc), false);
  }

  // big bang visual testing
  void testBigBang(Tester t) {
    LightEmAll game = new LightEmAll(4, 3, 0, 0, 5, false);
    int worldWidth = game.width * WorldConstants.CELL_SIZE;
    int worldHeight = game.height * WorldConstants.CELL_SIZE;
    game.bigBang(worldWidth, worldHeight, 0.5);
  }

  /*
   * // tests moving the powerStation void testOnMousePressed(Tester t) {
   * initData(); game1.onMousePressed((1,7), "LeftButton");
   * t.checkExpect(game1.board.get(1).get(7), piece1.rotate()); }
   */


  // testing moving the powerstation
  void testonKeyEvent(Tester t) {
    initData();
    game1.onKeyEvent("top");
    t.checkExpect(game1.board.get(0).get(5), false);
  }

  // testing if the bfs
  void testwiresConnectedToPsBfs(Tester t) {
    initData();
    game1.wiresConnectedToPsBfs();
    t.checkExpect(game1.board, false);
  }


  // testing to place powerstation
  void TestplacePs(Tester t) {
    initData();
    game0.placePs();
    t.checkExpect(this.startingp.powerStation, true);
    t.checkExpect(this.startingp.powered, true);
  }
  
  

}
