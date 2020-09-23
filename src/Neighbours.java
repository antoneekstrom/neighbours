import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static java.lang.Math.round;
import static java.lang.Math.sqrt;
import static java.lang.System.*;

/*
 *  Program to simulate segregation.
 *  See : http://nifty.stanford.edu/2014/mccown-schelling-model-segregation/
 *
 * NOTE:
 * - JavaFX first calls method init() and then the method start() far below.
 * - The method updateWorld() is called periodically by a Java timer.
 * - To test uncomment call to test() first in init() method!
 *
 */
// Extends Application because of JavaFX (just accept for now)
public class Neighbours extends Application {

    // Enumeration type for the Actors
    enum Actor {
        BLUE, RED, NONE   // NONE used for empty locations
    }

    // Enumeration type for the state of an Actor
    enum State {
        UNSATISFIED,
        SATISFIED,
        BLUESATISFIED,
        REDSATISIFED,
        NA     // Not applicable (NA), used for NONEs
    }

    // Below is the *only* accepted instance variable (i.e. variables outside any method)
    // This variable may *only* be used in methods init() and updateWorld()
    Actor[][] world;
    final Random rand = new Random();// The world is a square matrix of Actors

    // This is the method called by the timer to update the world
    // (i.e move unsatisfied) approx each 1/60 sec.
    void updateWorld() {
        // % of surrounding neighbours that are like me
        final double threshold = 0.3;
        State[][] states = getStates(world, threshold);
        moveUnsatisfied(world, states);
        // TODO Update logical state of world
    }


    // This method initializes the world variable with a random distribution of Actors
    // Method automatically called by JavaFX runtime (before graphics appear)
    // Don't care about "@Override" and "public" (just accept for now)
    @Override
    public void init() {
        //test();    // <---------------- Uncomment to TEST!

        // %-distribution of RED, BLUE and NONE
        double[] dist = {0.25, 0.25, 0.50};
        // Number of locations (places) in world (square)
        int nLocations = 90000;
        world = createWorld(world, nLocations);
        // TODO Create and populate world
        world = populateWorld(world, dist);
        // Should be last
        fixScreenSize(nLocations);
    }

    Actor[][] createWorld(Actor[][] world, int nLocations) {
        int len = (int) sqrt(nLocations);
        world = new Actor[len][len];
        return world;

    }

    Actor[][] populateWorld(Actor[][] world, double[] dist)
    {
        Arrays.stream(world).forEach(a -> Arrays.fill(a, Actor.NONE));
        Actor temp;
        int count = 0;
        for(int i=0; i < dist[0] * world.length; i++)
        {
            for(int j=0; j < world.length; j++)
            {
                int randomRow = rand.nextInt(world.length);
                int randomCol = rand.nextInt(world.length);
                world[i][j] = Actor.RED;
                temp = world[i][j];
                world[i][j] = world[randomRow][randomCol];
                world[randomRow][randomCol] = temp;
            }
            count++;
        }
        for(int i=count; i < dist[1] * world.length + count; i++)
        {
            for(int j=0; j < world.length; j++)
            {
                int randomRow = rand.nextInt(world.length);
                int randomCol = rand.nextInt(world.length);
                world[i][j] = Actor.BLUE;
                temp = world[i][j];
                world[i][j] = world[randomRow][randomCol];
                world[randomRow][randomCol] = temp;

            }

        }
        return world;
    }

    State[][] getStates(Actor[][] world, double threshold)
    {
        State[][] states = new State[world.length][world.length];
        for (int y = 0; y < world.length; y++)
        {
            for(int x = 0; x < world.length; x++)
            {
                states[y][x] = checkState(world, threshold, y, x);
                if(states[y][x] == State.UNSATISFIED) {
                    out.println(states[y][x]);
                }
            }
        }
        return states;
    }

    void moveUnsatisfied(Actor[][] world, State[][] states)
    {
        Actor temp;
        ArrayList<Integer> blueSatisifed = new ArrayList<>();
        for(int x=0; x < states.length; x++)
        {
            for(int y=0; y < states[x].length; y++)
            {
                if(states[x][y] == State.UNSATISFIED)
                {
                    int randomRow = rand.nextInt(world.length);
                    int randomCol = rand.nextInt(world.length);
                    while(world[randomRow][randomCol] == Actor.RED || world[randomRow][randomCol] == Actor.BLUE)
                    {
                        randomRow = rand.nextInt(world.length);
                        randomCol = rand.nextInt(world.length);
                    }
                    temp = world[x][y];
                    world[x][y] = world[randomRow][randomCol];
                    world[randomRow][randomCol] = temp;
                }
            }
        }
    }
    double getNeighbours(Actor[][] world, int x, int y)
    {
        double sameAgent = 0.0;
        double totalNeighbours = 0.0;
        for(int row = x - 1; row <= x + 1; row++)
        {
            for(int col = y - 1; col <= y + 1; col++)
            {
                if( !(row == x && col == y) && isValidLocation(world.length, row, col))
                {
                    totalNeighbours++;
                    if(world[row][col] == world[x][y])
                    {
                        sameAgent++;
                    }
                }
            }
        }
        return sameAgent / totalNeighbours;
    }

    //---------------- Methods ----------------------------
    State checkState(Actor[][] world, double threshold, int x, int y)
    {
        out.println(getNeighbours(world, x, y));
        if(world[x][y] == Actor.NONE)
        {
            world[x][y] = Actor.BLUE;
            if(getNeighbours(world, x , y) >= threshold )
            {
                world[x][y] = Actor.NONE;
                return State.BLUESATISFIED;
            }
            world[x][y] = Actor.RED;
            if(getNeighbours(world, x , y) >= threshold )
            {
                world[x][y] = Actor.NONE;
                return State.REDSATISIFED;
            }

            return State.NA;
        }
        else if(getNeighbours(world, x, y) >= threshold){
            return State.SATISFIED;
        }
        else {
            return State.UNSATISFIED;
        }
    }
    // Check if inside world
    boolean isValidLocation(int size, int row, int col) {
        return 0 <= row && row < size &&
                0 <= col && col < size;
    }


    // ------- Testing -------------------------------------

    // Here you run your tests i.e. call your logic methods
    // to see that they really work
    void test() {
        // A small hard coded world for testing
        Actor[][] testWorld = new Actor[][]{
                {Actor.RED, Actor.RED, Actor.NONE},
                {Actor.NONE, Actor.BLUE, Actor.NONE},
                {Actor.RED, Actor.NONE, Actor.BLUE}
        };
        double th = 0.5;   // Simple threshold used for testing

        int size = testWorld.length;
        out.println(isValidLocation(size, 0, 0));
        out.println(!isValidLocation(size, -1, 0));
        out.println(!isValidLocation(size, 0, 3));
        out.println(isValidLocation(size, 2, 2));

        // TODO More tests

        exit(0);
    }

    // Helper method for testing (NOTE: reference equality)
    <T> int count(T[] arr, T toFind) {
        int count = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == toFind) {
                count++;
            }
        }
        return count;
    }

    // ###########  NOTHING to do below this row, it's JavaFX stuff  ###########

    double width = 400;   // Size for window
    double height = 400;
    long previousTime = nanoTime();
    final long interval = 150000000; // 1 was 4
    double dotSize;
    final double margin = 50;

    void fixScreenSize(int nLocations) {
        // Adjust screen window depending on nLocations
        dotSize = (width - 2 * margin) / sqrt(nLocations);
        if (dotSize < 1) {
            dotSize = 2;
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        // Build a scene graph
        Group root = new Group();
        Canvas canvas = new Canvas(width, height);
        root.getChildren().addAll(canvas);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Create a timer
        AnimationTimer timer = new AnimationTimer() {
            // This method called by FX, parameter is the current time
            public void handle(long currentNanoTime) {
                long elapsedNanos = currentNanoTime - previousTime;
                if (elapsedNanos > interval) {
                    updateWorld();
                    renderWorld(gc, world);
                    previousTime = currentNanoTime;
                }
            }
        };

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Segregation Simulation");
        primaryStage.show();

        timer.start();  // Start simulation
    }


    // Render the state of the world to the screen
    public void renderWorld(GraphicsContext g, Actor[][] world) {
        g.clearRect(0, 0, width, height);
        int size = world.length;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                double x = dotSize * col + margin;
                double y = dotSize * row + margin;

                if (world[row][col] == Actor.RED) {
                    g.setFill(Color.RED);
                } else if (world[row][col] == Actor.BLUE) {
                    g.setFill(Color.BLUE);
                } else {
                    g.setFill(Color.WHITE);
                }
                g.fillOval(x, y, dotSize, dotSize);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
