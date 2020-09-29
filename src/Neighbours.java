import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.*;

import static java.lang.Math.*;
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
        NA     // Not applicable (NA), used for NONEs
    }

    // Below is the *only* accepted instance variable (i.e. variables outside any method)
    // This variable may *only* be used in methods init() and updateWorld()
    Actor[][] world;              // The world is a square matrix of Actors

    // This is the method called by the timer to update the world
    // (i.e move unsatisfied) approx each 1/60 sec.
    void updateWorld() {
        // % of surrounding neighbours that are like me
        final double threshold = 0.7;

        // Map the world to an array of representing their states
        State[][] states = getStates(world, threshold);
        // Move all the actors in an unsatisified state to a new (random) location
        moveActors(world, states);
    }

    // This method initializes the world variable with a random distribution of Actors
    // Method automatically called by JavaFX runtime (before graphics appear)
    // Don't care about "@Override" and "public" (just accept for now)
    @Override
    public void init() {
        //test();    // <---------------- Uncomment to TEST!

        // %-distribution of RED, BLUE and NONE
        double[] dist = {0.4, 0.4, 0.20};
        // Number of locations (places) in world (square)
        int nLocations = 90000;

        // Create world
        int len = (int)sqrt(nLocations); // Take squareroot of nLocations to get length of the sides
        world = new Actor[len][len];

        // Fill the world with actors
        populate(world, dist);

        // Should be last
        fixScreenSize(nLocations);
    }


    //---------------- Methods ----------------------------

    /**
     * Move all actors in an unsatisified state to a new random location in the world.
     * @param world a matrix storing the locations of the actors
     * @param states a matrix the same size as {@code world} which contains their states
     */
    void moveActors(Actor[][] world, State[][] states) {
        // Originally used lists here, which would've made it a little cleaner
        int[][] emptySpots = null; // Will contain all the currently empty locations in world
        int emptySpotsIndex = 0; // To keep track of where in the array we are currently looking

        // Get an array of all locations [x, y] in world
        int[][] locations = getLocations(world);
        shuffle(locations);

        // Iterate over all the locations (in a shuffled order)
        for (int[] location : locations) {
            int x = location[0];
            int y = location[1];

            // Only look for unsatisified actors
            if (states[y][x] != State.UNSATISFIED) {
                continue;
            }

            // Find new empty spots if our current array has been depleted or not created yet
            if (emptySpots == null || emptySpotsIndex >= emptySpots.length - 1) {
                emptySpots = findEmptyLocations(world);
                shuffle(emptySpots);
                emptySpotsIndex = 0; // Reset array pointer
            }

            // Get the next empty spot in the array, and then look at the next one
            int[] emptySpot = emptySpots[emptySpotsIndex];
            emptySpotsIndex++;

            if (emptySpot != null) {
                // Move the unsatisfied actor in the current location to the empty spot
                moveActor(world, location, emptySpot);
            }
        }
    }

    /**
     * Helper method to generate an array of all locations {@code [x, y]} in a matrix
     * @param matrix matrix to get the positions of
     * @param <T> the type of array
     * @return an array of {@code [x, y]} positions
     */
    <T> int[][] getLocations(T[][] matrix) {
        int[][] positions = new int[matrix.length * matrix[0].length][2];

        for (int y = 0; y < matrix.length; y++) {
            for (int x = 0; x < matrix[0].length; x++) {
                positions[x + y * matrix[0].length] = new int[] {x, y};
            }
        }

        return positions;
    }

    /**
     * Iterate over {@code world} to find all locations where {@link Actor} is {@code NONE}.
     * @param world matrix of actors
     * @return an array of {@code [x, y]} positions
     */
    int[][] findEmptyLocations(Actor[][] world) {
        // An array of length nLocations containing [x, y] arrays
        int[][] locations = new int[world.length * world[0].length][2];
        int locationsIndex = 0; // Current index of array we are looking at

        for (int y = 0; y < world.length; y++) {
            for (int x = 0; x < world[0].length; x++) {
                if (world[y][x] == Actor.NONE) { // Add to locations if location is empty
                    locations[locationsIndex] = new int[] {x, y};
                    locationsIndex++;
                }
            }
        }

        // Because we did not know in advance how many empty spots there would be,
        // a large portion of the original array is empty ( great place to use ArrayList :) )
        int[][] shortLocations = new int[locationsIndex][2];

        // Now we know that the number of empty locations is locationsIndex and we can make a new array of that length
        for (int i = 0; i < shortLocations.length; i++) {
            shortLocations[i] = locations[i];
        }

        return shortLocations;
    }

    /**
     * Move an actor to a new location.
     * @param world the matrix in which to move the actor
     * @param origin location of the actor to move
     * @param destination destination to move to the actor to
     */
    void moveActor(Actor[][] world, int[] origin, int[] destination) {
        world[destination[1]][destination[0]] = world[origin[1]][origin[0]];
        world[origin[1]][origin[0]] = Actor.NONE;
    }

    /**
     * Determine the states of the actors in a matrix.
     * @param world matrix of actors
     * @param threshold threshold for proportion of same-color neighbours in order for the actor to be satisified
     */
    State[][] getStates(Actor[][] world, double threshold) {
        State[][] states = new State[world.length][world[0].length];

        for (int y = 0; y < states.length; y++) {
            for (int x = 0; x < states[0].length; x++) {
                Actor[] neighbours = getNeighbours(world, world.length, x, y);
                float[] dist = getDistribution(neighbours);
                states[y][x] = determineState(world[y][x], dist, threshold);
            }
        }

        return states;
    }

    /**
     * Get the surrounding neighbours of an actor.
     * @param world matrix of actors
     * @param nLocations total number of locations in {@code world}, which equates to {@code rows * columns}.
     * @param x coordinate on the x-axis of the actor (columns)
     * @param y coordinate on the y-axis of the actor (rows)
     * @return an array of the neighbours (length 8)
     */
    Actor[] getNeighbours(Actor[][] world, int nLocations, int x, int y) {
        Actor[] neighbours = new Actor[8];
        int currentIndex = 0;

        // Iterate over a 3x3 submatrix centered on the given location
        for (int yOff = -1; yOff <= 1; yOff++) {
            for (int xOff = -1; xOff <= 1; xOff++) {

                if (xOff == 0 && yOff == 0) { // Skip the actor in the middle (it could not be its own neighbour)
                    continue;
                }

                // Compute the absolute location of the neighbour
                int xPos = xOff + x;
                int yPos = yOff + y;

                if (isValidLocation(nLocations, xPos, yPos)) { // Make sure the location is valid
                    neighbours[currentIndex] = world[yPos][xPos];
                }
                else {
                    neighbours[currentIndex] = Actor.NONE;
                }

                currentIndex++;
            }
        }

        return neighbours;
    }

    /**
     * Returns array of length 2 where index 0 is percent red actors and index 1 is percent blue actors.
     * @param actors an array of actors
     * @return the distribution
     */
    float[] getDistribution(Actor[] actors) {
        float numActors = actors.length - count(actors, Actor.NONE);
        return new float[] {
                count(actors, Actor.RED) / numActors,
                count(actors, Actor.BLUE) / numActors
        };
    }

    /**
     * Determine the state of an actor based on the distribution of it's neighbours.
     * @param a the actor
     * @param neighbourDist the distribution of it's neighbours
     * @param threshold threshold for proportion of same-color neighbours in order for the actor to be satisified
     * @return state of the actor
     */
    State determineState(Actor a, float[] neighbourDist, double threshold) {
        if (a == Actor.NONE) {
            return State.NA;
        }
        else {
            float percentLikeMe = neighbourDist[a == Actor.BLUE ? 1 : 0];
            return percentLikeMe >= threshold ? State.SATISFIED : State.UNSATISFIED;
        }
    }

    /**
     * Shuffles an array using the Fisher-Yates method.
     * @param array to shuffle
     * @return the same array, but shuffled
     */
    <T> T[] shuffle(T[] array) {
        return shuffle(array, new Random());
    }

    /**
     * Shuffles an array using the Fisher-Yates method. Mutates the original array.
     * @param array to shuffle
     * @param r an instance of {@link Random}
     * @return the same array, but shuffled
     */
    <T> T[] shuffle(T[] array, Random r) {

        for (int i = 0; i <= array.length - 2; i++) {
            int j = i + r.nextInt(array.length - i); // random integer such that i ≤ j < n
            T temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }

        return array; // return the same array for chain-like purposes
    }

    /**
     * Make a pool of actors with a certain distribution.
     * @param dist the distribution of actor-colors.
     * @param nLocations amount of actors
     * @return the array of actors
     */
    Actor[] makeActors(double[] dist, int nLocations) {
        Actor[] actors = new Actor[nLocations];

        int numRed = (int) (dist[0] * nLocations);
        int numBlue = (int) (dist[1] * nLocations);

        for (int i = 0; i < nLocations; i++) {
            if (i <= numRed) {
                actors[i] = Actor.RED;
            }
            else if (i <= numRed + numBlue) {
                actors[i] = Actor.BLUE;
            }
            else {
                actors[i] = Actor.NONE;
            }
        }

        return actors;
    }

    /**
     * Put a shuffle pool of actors with a certain distribution into a matrix.
     * @param world the matrix
     * @param dist the distribution
     * @return the same matrix, but populated
     */
    Actor[][] populate(Actor[][] world, double[] dist) {
        Actor[] actors = makeActors(dist, world.length * world[0].length);
        shuffle(actors); // Shuffle those actors in a random order

        for (int y = 0; y < world.length; y++) {
            for (int x = 0; x < world[0].length; x++) {
                world[y][x] = actors[x * y];
            }
        }

        return world;
    }

    /**
     * Check if a location would be contained within a matrix of a certain size.
     * @param size size of the matrix
     * @param row coordinate on the x-axis of the location
     * @param col coordinate on the y-axis of the location
     * @return of the location is valid
     */
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

        out.println(isValidLocation(testWorld.length, 0, 0));
        out.println(!isValidLocation(testWorld.length, -1, 0));
        out.println(!isValidLocation(testWorld.length, 0, 3));
        out.println(isValidLocation(testWorld.length, 2, 2));

        double th = 0.5;
        double[] dist = {0.4, 0.4, 0.2};
        int nLocations = 90000;
        int size = (int) Math.sqrt(nLocations);

        // Test distribution of actors
        int numRed = 0, numBlue = 0, numEmpty  = 0;
        Actor[][] world = populate(new Actor[size][size], dist);

        // count actual number of agents being created
        for (Actor[] row : world) {
            for (Actor a : row) {
                if (a == Actor.RED) {
                    numRed++;
                }
                else if (a == Actor.BLUE) {
                    numBlue++;
                }
                else {
                    numEmpty++;
                }
            }
        }

        // compare with correct distribution
        out.println(numRed == dist[0] * nLocations);
        out.println(numBlue == dist[1] * nLocations);
        out.println(numEmpty == dist[2] * nLocations);


        // Test findEmptySpots
        int[][] emptySpots = findEmptyLocations(world);
        shuffle(emptySpots);

        for (int i = 0; i < 5; i++) {
            int[] spot = emptySpots[i];
            out.println(world[spot[1]][spot[0]] == Actor.NONE);
        }

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

    double width = 600;   // Size for window
    double height = 600;
    long previousTime = nanoTime();
    final long interval = 450000000;
    double dotSize;
    final double margin = 25;

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
                g.fillRect(x, y, dotSize, dotSize);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
