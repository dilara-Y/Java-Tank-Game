
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import java.util.*;

/**
 *Main.class for gameplay settings and process
 * necessary constants, game scene , images
 */

public class Main extends Application {
    // Game constants
    private static final int VIEW_WIDTH = 800;   // Visible screen width
    private static final int VIEW_HEIGHT = 600;  // Visible screen height
    private static final int MAP_WIDTH = 1200;   // Total map width is 1.5 times the visible screen width
    private static final int MAP_HEIGHT = 960;   // Total map height is 1.5 times the visible screen height
    private static final int TANK_SIZE = 40;
    private static final int BULLET_SIZE = 10;
    private static final int WALL_BLOCK_SIZE = 20;
    private static final int PLAYER_SPEED = 2;
    private static final int ENEMY_SPEED = 2;
    private static final int BULLET_SPEED = 7;
    private static final int MAX_ENEMIES = 5;    // I think it is enough.

    // Game states
    private enum GameState { RUNNING, PAUSED, GAME_OVER }
    private GameState gameState = GameState.RUNNING;
    private int score = 0;
    private int lives = 3;

    // Game objects
    private PlayerTank player;
    private final List<EnemyTank> enemies = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<WallBlock> wallBlocks = new ArrayList<>();
    private final List<Explosion> explosions = new ArrayList<>();

    // Images
    private Image playerTank1, playerTank2, enemyTank1, enemyTank2,bulletImage,wallBlockImage,explosionImage,smallExplosionImage;

    // Animation variables
    private long lastFrameTime = 0;
    private boolean tankFrame = false;

    // Random generator
    private Random random = new Random();


    private Canvas canvas;
    private GraphicsContext gc;

    //For checking respawn processing
    private long lastEnemySpawnTime = 0;
    private static final long ENEMY_SPAWN_INTERVAL = 5000;
    private boolean waitingForRespawn = false;
    private long respawnTime = 0;


    // Visible area coordinates
    private double viewportX = 0;
    private double viewportY = 0;

    //Directions
    private enum Direction { UP, DOWN, LEFT, RIGHT }

    /**
     *@param primaryStage the primary stage for this application
     * Creating Scene
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            //Loading ımages
            playerTank1 = new Image(getClass().getResourceAsStream("/assets/yellowTank1.png"));
            playerTank2 = new Image(getClass().getResourceAsStream("/assets/yellowTank2.png"));
            enemyTank1 = new Image(getClass().getResourceAsStream("/assets/whiteTank1.png"));
            enemyTank2 = new Image(getClass().getResourceAsStream("/assets/whiteTank2.png"));
            bulletImage = new Image(getClass().getResourceAsStream("/assets/bullet.png"));
            wallBlockImage = new Image(getClass().getResourceAsStream("/assets/wall.png"));
            explosionImage = new Image(getClass().getResourceAsStream("/assets/explosion.png"));
            smallExplosionImage = new Image(getClass().getResourceAsStream("/assets/smallExplosion.png"));
        } catch (Exception e) {
            System.err.println("Error loading images: " + e.getMessage());
        }

        Pane root = new Pane();
        canvas = new Canvas(VIEW_WIDTH, VIEW_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        root.getChildren().add(canvas);

        initializeGame();

        Scene scene = new Scene(root, VIEW_WIDTH, VIEW_HEIGHT);
        scene.setOnKeyPressed(this::handleKeyPress);
        scene.setOnKeyReleased(this::handleKeyRelease);

        primaryStage.setTitle("Tank2025");
        primaryStage.setScene(scene);
        primaryStage.show();

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                update(now);
                render(now);
            }
        }.start();
    }

    /**
     * Player Tank initializing
     */
    private void initializeGame() {
        // Creating player tank, now starting more centered and facing right by default
        player = new PlayerTank(MAP_WIDTH / 2 - TANK_SIZE / 2, MAP_HEIGHT / 2 - TANK_SIZE / 2, Direction.RIGHT);

        // Center viewport on player
        viewportX = player.x - VIEW_WIDTH / 2;
        viewportY = player.y - VIEW_HEIGHT / 2;

        // Make sure viewport stays within map bounds
        clampViewport();
        createMapLayout();

        // Creating initial enemies
        for (int i = 0; i < 3; i++) {
            spawnEnemy();
        }
    }

    private void clampViewport() {
        //Make sure view stays within map bounds
        viewportX = Math.max(0, Math.min(MAP_WIDTH - VIEW_WIDTH, viewportX));
        viewportY = Math.max(0, Math.min(MAP_HEIGHT - VIEW_HEIGHT, viewportY));
    }

    /**
     * Custom Wall Design and Boundary Walls
     */
    private void createMapLayout() {

        wallBlocks.clear();

        // Boundary Wall Design
        for (int x = 0; x < MAP_WIDTH; x += WALL_BLOCK_SIZE) {
            wallBlocks.add(new WallBlock(x, 0)); // Top
            wallBlocks.add(new WallBlock(x, MAP_HEIGHT - WALL_BLOCK_SIZE)); // Bottom
        }
        for (int y = 0; y < MAP_HEIGHT; y += WALL_BLOCK_SIZE) {
            wallBlocks.add(new WallBlock(0, y)); // Left
            wallBlocks.add(new WallBlock(MAP_WIDTH - WALL_BLOCK_SIZE, y)); // Right
        }
        // Custom Wall Design
        for (int x = MAP_WIDTH/2 - 150; x <= MAP_WIDTH/2 + 150; x += WALL_BLOCK_SIZE) {
            wallBlocks.add(new WallBlock(x, MAP_HEIGHT/3 - 20));
        }
        for (int y = MAP_HEIGHT/2 - 60; y <= MAP_HEIGHT/2 + 100; y += WALL_BLOCK_SIZE) {
            wallBlocks.add(new WallBlock(MAP_WIDTH/4 - 20, y));
            wallBlocks.add(new WallBlock(MAP_WIDTH/4 , y));
            wallBlocks.add(new WallBlock(3*MAP_WIDTH/4 - 20, y));
            wallBlocks.add(new WallBlock(3*MAP_WIDTH/4 , y));
        }

        for (int y = MAP_HEIGHT/2 - 60; y <= MAP_HEIGHT/2 + 300; y += WALL_BLOCK_SIZE) {
            wallBlocks.add(new WallBlock(MAP_WIDTH/6 - 20, y-100));
            wallBlocks.add(new WallBlock(MAP_WIDTH/6 , y-100));
            wallBlocks.add(new WallBlock(5*MAP_WIDTH/6 - 20, y-100));
            wallBlocks.add(new WallBlock(5*MAP_WIDTH/6 , y-100));
        }
    }

    private void update(long now) {
        // For setting delay respawn or game over after explosion
        if (waitingForRespawn) {
            if (System.nanoTime() > respawnTime) {
                waitingForRespawn = false;
                if (lives <= 0) {
                    gameState = GameState.GAME_OVER;
                } else {
                    player.respawn();//İf player still have lives
                }
            }
            return;
        }

        //Tank animation
        if (now - lastFrameTime > 200_000_000) {
            tankFrame = !tankFrame;
            lastFrameTime = now;
        }

        if (gameState != GameState.RUNNING) return;

        // Update player
        player.update();

        // Update viewport based on player position
        updateViewport();

        // Check tank-tank collisions
        for (int i = enemies.size() - 1; i >= 0; i--) {
            EnemyTank enemy = enemies.get(i);
            // Collidable interface intersects method
            if (player.intersects(enemy)) {
                explosions.add(new Explosion(player.x, player.y, true));
                explosions.add(new Explosion(enemy.x, enemy.y, true));
                enemies.remove(i);
                lives--;
                waitingForRespawn = true;
                respawnTime = System.nanoTime() + 1_000_000_000;
                return;
            }

        }

        // Update enemies
        for (EnemyTank enemy : enemies) {
            enemy.update();
        }

        // Update bullets
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            bullet.update();

            // For checking if bullet is out of bounds
            if (bullet.x < 0 || bullet.x > MAP_WIDTH || bullet.y < 0 || bullet.y > MAP_HEIGHT) {
                bullets.remove(i);
                continue;
            }

            // For checking bullet collision with walls
            boolean bulletRemoved = false;
            for (WallBlock wall : wallBlocks) {
                // Collidable interface intersects method
                if (bullet.intersects(wall)) {
                    explosions.add(new Explosion(bullet.x, bullet.y, false));
                    bullets.remove(i);
                    bulletRemoved = true;
                    break;
                }
            }
            if (bulletRemoved) {
                continue;
            }

            // For checking bullet collision with player
            // Collidable interface intersects method
            if (bullet.shooter != player && bullet.intersects(player)) {
                // Bullet hits player
                explosions.add(new Explosion(player.x, player.y, true));
                bullets.remove(i);
                lives--;
                waitingForRespawn = true;
                respawnTime = System.nanoTime() + 1_000_000_000; //delay
                continue;

            }

            // For checking bullet collision with enemies
            for (int j = enemies.size() - 1; j >= 0; j--) {
                EnemyTank enemy = enemies.get(j);
                // Collidable interface intersects method
                if (bullet.shooter == player && bullet.intersects(enemy)) {
                    explosions.add(new Explosion(enemy.x, enemy.y, true));
                    bullets.remove(i);
                    enemies.remove(j);
                    score += 100;
                    break;
                }
            }
        }

        // For removing explosions that should be finished
        explosions.removeIf(Explosion::isFinished);

        // Spawn new enemies
        if (enemies.size() < MAX_ENEMIES && now - lastEnemySpawnTime > ENEMY_SPAWN_INTERVAL) {
            spawnEnemy();
            lastEnemySpawnTime = now;
        }
    }

    /**
     * For Vertical and Horizontal Scrolling
     */
    private void updateViewport() {
        // Calculating how far the player is from the center of the screen
        double targetViewportX = player.x - (VIEW_WIDTH / 2.0);
        double targetViewportY = player.y - (VIEW_HEIGHT / 2.0);
        viewportX += (targetViewportX - viewportX) * 0.1;
        viewportY += (targetViewportY - viewportY) * 0.1;

        clampViewport();
    }

    // Random enemy spawn
    private void spawnEnemy() {
        int x = random.nextInt(MAP_WIDTH - TANK_SIZE - WALL_BLOCK_SIZE * 2) + WALL_BLOCK_SIZE;
        int y = WALL_BLOCK_SIZE + 10;
        Direction[] directions = {Direction.LEFT, Direction.RIGHT, Direction.DOWN};
        Direction randomDir = directions[random.nextInt(3)];
        enemies.add(new EnemyTank(x, y, randomDir));
    }

    private void render(long now) {
        // Clear
        gc.clearRect(0, 0, VIEW_WIDTH, VIEW_HEIGHT);

        // Draw background
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, VIEW_WIDTH, VIEW_HEIGHT);

        // Draw walls (only those in viewport)
        for (WallBlock wall : wallBlocks) {
            if (isInViewport(wall.x, wall.y, WALL_BLOCK_SIZE, WALL_BLOCK_SIZE)) {
                double screenX = wall.x - viewportX;
                double screenY = wall.y - viewportY;
                gc.drawImage(wallBlockImage, screenX, screenY, WALL_BLOCK_SIZE, WALL_BLOCK_SIZE);
            }
        }

        // Draw player if in viewport
        if (!waitingForRespawn && isInViewport(player.x, player.y, TANK_SIZE, TANK_SIZE)) {
            player.draw(gc, tankFrame, viewportX, viewportY);
        }

        // Draw enemies if in viewport
        for (EnemyTank enemy : enemies) {
            if (isInViewport(enemy.x, enemy.y, TANK_SIZE, TANK_SIZE)) {
                enemy.draw(gc, tankFrame, viewportX, viewportY);
            }
        }

        // Draw bullets with rotation if in viewport
        for (Bullet bullet : bullets) {
            if (isInViewport(bullet.x, bullet.y, BULLET_SIZE, BULLET_SIZE)) {
                drawRotatedBullet(bullet, viewportX, viewportY);
            }
        }

        // Draw explosions if in viewport
        for (Explosion explosion : explosions) {
            if (isInViewport(explosion.x, explosion.y,
                    explosion.isBig ? TANK_SIZE * 2 : TANK_SIZE,
                    explosion.isBig ? TANK_SIZE * 2 : TANK_SIZE)) {
                explosion.draw(gc, viewportX, viewportY);
            }
        }

        // Draw score and lives
        gc.setFill(Color.WHITE);
        gc.setFont(new Font(20));
        gc.fillText("Score: " + score, 20, 30);
        gc.fillText("Lives: " + lives, 20, 60);

        // Draw pause/game over screen
        if (gameState == GameState.PAUSED) {
            drawPauseScreen();
        } else if (gameState == GameState.GAME_OVER) {
            drawGameOverScreen();
        }
    }

    private boolean isInViewport(double x, double y, double width, double height) {
        return x + width > viewportX &&
                x < viewportX + VIEW_WIDTH &&
                y + height > viewportY &&
                y < viewportY + VIEW_HEIGHT;
    }

    private void drawRotatedBullet(Bullet bullet, double viewportX, double viewportY) {
        gc.save(); // Save the current graphics state

        //For calculating screen position
        double screenX = bullet.x - viewportX + BULLET_SIZE/2;
        double screenY = bullet.y - viewportY + BULLET_SIZE/2;

        // Move to bullet's center point
        gc.translate(screenX, screenY);

        // Rotate based on direction
        switch (bullet.direction) {
            case UP:
                gc.rotate(-90);
                break;
            case DOWN:
                gc.rotate(90);
                break;
            case LEFT:
                gc.rotate(180);
                break;
            case RIGHT:
                break;
        }

        // Draw the bullet image (centered)
        gc.drawImage(bulletImage, -BULLET_SIZE/2, -BULLET_SIZE/2, BULLET_SIZE, BULLET_SIZE);

        gc.restore(); // Restore the graphics context state
    }
    /**Draw Pause Screen method*/
    private void drawPauseScreen() {
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRect(0, 0, VIEW_WIDTH, VIEW_HEIGHT);

        gc.setFill(Color.WHITE);
        gc.setFont(new Font(40));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("PAUSED", VIEW_WIDTH / 2, VIEW_HEIGHT / 2 - 40);

        gc.setFont(new Font(20));
        gc.fillText("Press R to Restart", VIEW_WIDTH / 2, VIEW_HEIGHT / 2 + 20);
        gc.fillText("Press ESC to Exit", VIEW_WIDTH / 2, VIEW_HEIGHT / 2 + 50);
        gc.fillText("Press P to Resume", VIEW_WIDTH / 2, VIEW_HEIGHT / 2 + 80);

        gc.setTextAlign(TextAlignment.LEFT);
    }
    /**Draw GameOver Screen method*/
    private void drawGameOverScreen() {
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRect(0, 0, VIEW_WIDTH, VIEW_HEIGHT);

        gc.setFill(Color.WHITE);
        gc.setFont(new Font(40));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("GAME OVER", VIEW_WIDTH / 2, VIEW_HEIGHT / 2 - 40);

        gc.setFont(new Font(30));
        gc.fillText("Final Score: " + score, VIEW_WIDTH / 2, VIEW_HEIGHT / 2 + 20);

        gc.setFont(new Font(20));
        gc.fillText("Press R to Restart", VIEW_WIDTH / 2, VIEW_HEIGHT / 2 + 70);
        gc.fillText("Press ESC to Exit", VIEW_WIDTH / 2, VIEW_HEIGHT / 2 + 100);

        gc.setTextAlign(TextAlignment.LEFT);
    }

    /**
     * For handling Key Press such as R, X, ESCAPE
     */
    private void handleKeyPress(KeyEvent event) {
        //For showing pressed key
        KeyCode code = event.getCode();
        System.out.println("Key pressed: " + code);
        if (gameState == GameState.GAME_OVER) {
            if (event.getCode() == KeyCode.R) {
                restartGame();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                System.exit(0);
            }
            return;
        }

        if (gameState == GameState.PAUSED) {
            if (event.getCode() == KeyCode.R) {
                restartGame();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                System.exit(0);
            } else if (event.getCode() == KeyCode.P) {
                gameState = GameState.RUNNING;
            }
            return;
        }

        // Handle game controls
        switch (event.getCode()) {
            case UP:
                player.setMovingUp(true);
                player.setDirection(Direction.UP);
                break;
            case DOWN:
                player.setMovingDown(true);
                player.setDirection(Direction.DOWN);
                break;
            case LEFT:
                player.setMovingLeft(true);
                player.setDirection(Direction.LEFT);
                break;
            case RIGHT:
                player.setMovingRight(true);
                player.setDirection(Direction.RIGHT);
                break;
            case X:
                Bullet bullet = player.shoot();
                if (bullet != null) {
                    bullets.add(bullet);
                }
                break;
            case P:
                gameState = GameState.PAUSED;
                break;
        }
    }

    private void handleKeyRelease(KeyEvent event) {
        switch (event.getCode()) {
            case UP:
                player.setMovingUp(false);
                break;
            case DOWN:
                player.setMovingDown(false);
                break;
            case LEFT:
                player.setMovingLeft(false);
                break;
            case RIGHT:
                player.setMovingRight(false);
                break;
        }
    }

    private void restartGame() {
        // Reset game state
        gameState = GameState.RUNNING;
        score = 0;
        lives = 3;

        // Clear all game objects
        enemies.clear();
        bullets.clear();
        explosions.clear();

        // Reinitialize game
        initializeGame();

        // Reset spawn timer
        lastEnemySpawnTime = System.nanoTime();
    }

    // Inner classes for game objects
    interface Collidable {

        double getX();
        double getY();
        double getWidth();
        double getHeight();

        default boolean intersects(Collidable other) {
            return this.getX() < other.getX() + other.getWidth() &&
                    this.getX() + this.getWidth() > other.getX() &&
                    this.getY() < other.getY() + other.getHeight() &&
                    this.getY() + this.getHeight() > other.getY();
        }
    }

    /**
     * Tank.java: superclass of PlayerTank and EnemyTank classes
     */
    abstract class Tank implements Collidable {
        double x, y;
        Direction direction;
        Image frame1, frame2;
        int speed;
        long lastShotTime = 0;
        long shootCooldown;

        public Tank(double x, double y, Direction direction, Image frame1, Image frame2, int speed, long shootCooldown) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.frame1 = frame1;
            this.frame2 = frame2;
            this.speed = speed;
            this.shootCooldown = shootCooldown;
        }

        // Collidable methods
        @Override
        public double getX() { return this.x; }
        @Override
        public double getY() { return this.y; }
        @Override
        public double getWidth() { return TANK_SIZE; }
        @Override
        public double getHeight() { return TANK_SIZE; }

        public void draw(GraphicsContext gc, boolean frame, double viewportX, double viewportY) {
            gc.save();
            double screenX = x - viewportX + TANK_SIZE / 2;
            double screenY = y - viewportY + TANK_SIZE / 2;
            gc.translate(screenX, screenY);

            switch (direction) {
                case UP: gc.rotate(-90); break;
                case DOWN: gc.rotate(90); break;
                case LEFT: gc.rotate(180); break;
                case RIGHT: break;
            }

            Image tankImage = frame ? frame1 : frame2;
            gc.drawImage(tankImage, -TANK_SIZE / 2, -TANK_SIZE / 2, TANK_SIZE, TANK_SIZE);
            gc.restore();
        }

        public Bullet shoot() {
            long now = System.nanoTime();
            if (now - lastShotTime < shootCooldown) {
                return null;
            }

            lastShotTime = now;
            double bulletX = x + TANK_SIZE / 2 - BULLET_SIZE / 2;
            double bulletY = y + TANK_SIZE / 2 - BULLET_SIZE / 2;
            double dx = 0, dy = 0;

            switch (direction) {
                case UP:
                    bulletY = y - BULLET_SIZE;
                    dy = -BULLET_SPEED;
                    break;
                case DOWN:
                    bulletY = y + TANK_SIZE;
                    dy = BULLET_SPEED;
                    break;
                case LEFT:
                    bulletX = x - BULLET_SIZE;
                    dx = -BULLET_SPEED;
                    break;
                case RIGHT:
                    bulletX = x + TANK_SIZE;
                    dx = BULLET_SPEED;
                    break;
            }

            return new Bullet(bulletX, bulletY, dx, dy, this, direction);
        }

        protected boolean canMove(double newX, double newY) {
            for (WallBlock wall : wallBlocks) {
                if (newX + TANK_SIZE > wall.getX() &&
                        newX < wall.getX() + wall.getWidth() &&
                        newY + TANK_SIZE > wall.getY() &&
                        newY < wall.getY() + wall.getHeight()) {
                    return false;
                }
            }
            return true;
        }

        protected void keepInBounds() {
            x = Math.max(WALL_BLOCK_SIZE, Math.min(MAP_WIDTH - WALL_BLOCK_SIZE - TANK_SIZE, x));
            y = Math.max(WALL_BLOCK_SIZE, Math.min(MAP_HEIGHT - WALL_BLOCK_SIZE - TANK_SIZE, y));
        }

        public abstract void update();
    }

    /**
     * PlayerTank.java
     * Override update and respawn methods for player tank
     */
    class PlayerTank extends Tank {
        private boolean movingUp = false;
        private boolean movingDown = false;
        private boolean movingLeft = false;
        private boolean movingRight = false;

        public PlayerTank(double x, double y, Direction direction) {
            super(x, y, direction, playerTank1, playerTank2, PLAYER_SPEED, 500_000_000);
        }

        @Override
        public void update() {
            double newX = x;
            double newY = y;

            if (movingUp) {
                newY -= speed;
                direction = Direction.UP;
            }
            if (movingDown) {
                newY += speed;
                direction = Direction.DOWN;
            }
            if (movingLeft) {
                newX -= speed;
                direction = Direction.LEFT;
            }
            if (movingRight) {
                newX += speed;
                direction = Direction.RIGHT;
            }

            if (canMove(newX, newY)) {
                x = newX;
                y = newY;
                keepInBounds();
            }
        }

        public void respawn() {
            x = MAP_WIDTH / 2 - TANK_SIZE / 2;
            y = MAP_HEIGHT / 2 - TANK_SIZE / 2;
            direction = Direction.RIGHT;
            viewportX = x - VIEW_WIDTH / 2;
            viewportY = y - VIEW_HEIGHT / 2;
            clampViewport();
        }

        public void setMovingUp(boolean movingUp) { this.movingUp = movingUp; }
        public void setMovingDown(boolean movingDown) { this.movingDown = movingDown; }
        public void setMovingLeft(boolean movingLeft) { this.movingLeft = movingLeft; }
        public void setMovingRight(boolean movingRight) { this.movingRight = movingRight; }
        public void setDirection(Direction direction) { this.direction = direction; }
    }
    /**
     * EnemyTank.java
     * Override update and respawn methods for enemy tanks
     */
    class EnemyTank extends Tank {
        private long lastDirectionChange = 0;

        public EnemyTank(double x, double y, Direction direction) {
            super(x, y, direction, enemyTank1, enemyTank2, ENEMY_SPEED, 5_000_000_000L);
        }

        @Override
        public void update() {
            long now = System.nanoTime();
            if (now - lastDirectionChange > 2_000_000_000) {
                Direction[] directions = {Direction.LEFT, Direction.RIGHT, Direction.DOWN};
                direction = directions[random.nextInt(directions.length)];
                lastDirectionChange = now;
            }

            double newX = x;
            double newY = y;

            switch (direction) {
                case UP: newY -= speed; break;
                case DOWN: newY += speed; break;
                case LEFT: newX -= speed; break;
                case RIGHT: newX += speed; break;
            }

            if (canMove(newX, newY)) {
                x = newX;
                y = newY;
                keepInBounds();
            } else {
                direction = Direction.values()[random.nextInt(Direction.values().length)];
                lastDirectionChange = System.nanoTime();
            }

            if (System.nanoTime() - lastShotTime > shootCooldown && random.nextInt(100) < 10) {
                bullets.add(shoot());
                lastShotTime = System.nanoTime();
            }
        }
    }


    /**
     * Bullet class
     * Override Collidable interface methods
     */
    class Bullet implements Collidable {
        double x, y;
        double dx, dy;
        Object shooter; // PlayerTank or EnemyTank
        Direction direction;

        public Bullet(double x, double y, double dx, double dy, Object shooter, Direction direction) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.shooter = shooter;
            this.direction = direction;
        }

        @Override
        public double getX() { return this.x; }
        @Override
        public double getY() { return this.y; }
        @Override
        public double getWidth() { return BULLET_SIZE; }
        @Override
        public double getHeight() { return BULLET_SIZE; }

        public void update() {
            x += dx;
            y += dy;
        }
    }

    /**
     * WallBlock class
     * Override Collidable interface methods
     */
    class WallBlock implements Collidable {
        double x, y;

        public WallBlock(double x, double y) {
            this.x = x;
            this.y = y;
        }

        // Collidable methods
        @Override
        public double getX() { return this.x; }
        @Override
        public double getY() { return this.y; }
        @Override
        public double getWidth() { return WALL_BLOCK_SIZE; }
        @Override
        public double getHeight() { return WALL_BLOCK_SIZE; }
    }

    /**
     * Explosion class
     * Processing collision's type and time
     */
    class Explosion {
        double x, y;
        long startTime;
        boolean isBig;
        boolean finished = false;

        public Explosion(double x, double y, boolean isBig) {
            this.x = x;
            this.y = y;
            this.isBig = isBig;
            this.startTime = System.nanoTime();
        }

        public void draw(GraphicsContext gc, double viewportX, double viewportY) {
            if (finished) return;

            Image image = isBig ? explosionImage : smallExplosionImage;
            double size = isBig ? TANK_SIZE * 2 : TANK_SIZE;

            // Calculate screen position
            double screenX = x - viewportX - size/2;
            double screenY = y - viewportY - size/2;

            gc.drawImage(image, screenX, screenY, size, size);

            // Mark as finished after a short delay
            if (System.nanoTime() - startTime > 500_000_000) {
                finished = true;
            }
        }

        public boolean isFinished() {
            return finished;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}