import javax.imageio.ImageIO;
import java.applet.Applet;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class Solver {


    public static void main(String[] args) {
        String screenshotFolderPath = args[0];
        String outFolderPath = args[1];
        System.out.println(screenshotFolderPath);
        System.out.println(outFolderPath);

        File oldest = null;
        File prev = null;
        while (true) {
            long oldestTimestamp = 0;
	        File folder = new File(screenshotFolderPath);
            for (File file : folder.listFiles()) {
                if (!file.isDirectory()) {
                    if (file.lastModified() > oldestTimestamp) {
                        oldestTimestamp = file.lastModified();
                        oldest = file;
                    }
                }
            }

            if (oldest != null && (prev == null || !oldest.getPath().equals(prev.getPath())) && (System.currentTimeMillis() - oldest.lastModified() < 1000 * 30)) {
		        System.out.println(oldest.getName());
                solve(oldest, outFolderPath + "SOLVED_" + oldest.getName());
                prev = oldest;
            }

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }


    public static void solve(File imageFile, String solutionPath) {
        BufferedImage image = null;
        BufferedImage colorImage = null;
        try {
            colorImage = ImageIO.read(imageFile);
            int size = colorImage.getHeight();
            colorImage = colorImage.getSubimage(colorImage.getWidth() / 2 - size / 2, colorImage.getHeight() / 2 - size / 2, size, size);
            image = Utils.getGrayScale(colorImage);


            int[][] visited = new int[image.getWidth()][image.getHeight()];
            
            for(int x = 0; x < image.getWidth(); x++)
                for(int y = 0; y < image.getHeight(); y++)
                    visited[x][y] = -1;

            int regionCount = 0;
            for(int x = 0; x < image.getWidth(); x++)
                for(int y = 0; y < image.getHeight(); y++)
                    if (visited[x][y] == -1) {
                        Utils.dfs(image, visited, x, y, regionCount);
                        regionCount++;
                    }
            //System.out.println(regionCount);

            int[] counts = new int[regionCount];
            int[] sum = new int[regionCount];
            Region[] regions = new Region[regionCount];
            for(int x = 0; x < image.getWidth(); x++)
                for(int y = 0; y < image.getHeight(); y++) {
                    counts[visited[x][y]]++;
                    sum[visited[x][y]] += Utils.getGS(image, x, y);
                }

            for(int i = 0; i < sum.length; i++) {
                if (counts[i] > 10) {
                    sum[i] = sum[i] / counts[i];
                } else
                    sum[i] = 255;
                regions[i] = new Region(i, sum[i]);
            }

            for(int x = 0; x < image.getWidth(); x++)
                for(int y = 0; y < image.getHeight(); y++) {
                    regions[visited[x][y]].addPoint(x, y);
                }

            for(int i = 0; i < regions.length; i++) {
                regions[i].calcStats();
                if (regions[i].isPuzzle()) {
//                    regions[i].printStats();

                    PuzzleDataExtractor extractor = new PuzzleDataExtractor(image, colorImage, regions[i]);
                    extractor.drawPuzzleBoundingBox(image);
                    extractor.drawPuzzleBoundingBox(colorImage);
                    extractor.drawPuzzleCellCenters(image);
                    extractor.drawPuzzleCellCenters(colorImage);

                    PuzzleSolver solver;
                    if (extractor.getPuzzleType() == PuzzleType.COLOR_PUZZLE) {
                        Puzzle puzzle = extractor.getColorPuzzle();
                        solver = new ColorPuzzleSolver(puzzle);
                        System.out.println("Color puzzle found");
                        for(int j = 0; j < 4; j++) {
                            System.out.println(Arrays.toString(puzzle.gridValues[j]));
                        }
                    } else {
                        Puzzle puzzle = extractor.getTrianglesPuzzle();
                        solver = new TrianglePuzzleSolver(puzzle);
                        for(int j = 0; j < 4; j++) {
                            System.out.println(Arrays.toString(puzzle.gridValues[j]));
                        }
                        System.out.println("Triangle puzzle found");
                    }
                    ArrayList<Point> solution = solver.solve();
                    extractor.drawPath(image, solution);
                    extractor.drawPath(colorImage, solution);
                    Utils.saveImage(solutionPath, image);

                }
            }
            System.out.println("============================");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
abstract class PuzzleSolver {
    int[][] grid;
    Point start, end;
    int h, w;

    boolean[][] points;
    ArrayList<Point> solution, path;

    public PuzzleSolver(Puzzle puzzle) {
        grid = puzzle.gridValues;
        h = grid.length;
        w = grid[0].length;
        start = new Point(h, 0);
        end = new Point(0, w);
    }

    abstract public ArrayList<Point> solve();
}
class TrianglePuzzleSolver extends PuzzleSolver{
    int[][] counts;

    public TrianglePuzzleSolver(Puzzle puzzle) {
        super(puzzle);
    }

    public ArrayList<Point> solve() {
        points = new boolean[h + 1][w + 1];
        counts = new int[h][w];
        path = new ArrayList<Point>();
        points[start.x][start.y] = true;
        search(start.x, start.y);
        System.out.println(solution == null ? "Solution not found" : "Length of path: " + path.size());
        return solution;
    }

    public boolean inside(int x, int y, int h, int w) {
        return !(x < 0 || x >= h || y < 0 || y >= w);
    }
    public boolean search(int x, int y) {
        if (x == end.x && y == end.y) {
            for(int i = 0; i < h; i++) 
                for(int j = 0; j < w; j++)
                    if (grid[i][j] > 0 && grid[i][j] != counts[i][j]) return false;
            solution = path;
            return true;
        }

        for(int[] d: Utils.delta) {
            int nx = x + d[0];
            int ny = y + d[1];

            if (!inside(nx, ny, h + 1, w + 1)) continue;
            if (points[nx][ny]) continue;
            int cell1X, cell1Y, cell2X, cell2Y;
            if (x == nx) {
                cell1X = nx - 1;
                cell1Y = Math.min(y, ny);
                cell2X = nx;
                cell2Y = Math.min(y, ny);
            } else {
                cell1X = Math.min(x, nx);
                cell1Y = ny - 1;
                cell2X = Math.min(x, nx);
                cell2Y = ny;
            }
            if (inside(cell1X, cell1Y, h, w) && grid[cell1X][cell1Y] > 0 && counts[cell1X][cell1Y] == grid[cell1X][cell1Y]) continue;
            if (inside(cell2X, cell2Y, h, w) && grid[cell2X][cell2Y] > 0 && counts[cell2X][cell2Y] == grid[cell2X][cell2Y]) continue;

            points[nx][ny] = true;
            if (inside(cell1X, cell1Y, h, w)) counts[cell1X][cell1Y]++;
            if (inside(cell2X, cell2Y, h, w)) counts[cell2X][cell2Y]++;
            path.add(new Point(d[1], d[0]));
            if (search(nx, ny)) return true;

            points[nx][ny] = false;
            if (inside(cell1X, cell1Y, h, w)) counts[cell1X][cell1Y]--;
            if (inside(cell2X, cell2Y, h, w)) counts[cell2X][cell2Y]--;
            path.remove(path.size() - 1);


        }

        return false;
    }
}


class ColorPuzzleSolver extends PuzzleSolver{
    Set<ArrayList<Integer>> edges;

    
    public ColorPuzzleSolver(Puzzle puzzle) {
        super(puzzle);
    }
    
    public ArrayList<Point> solve() {
        points = new boolean[h + 1][w + 1];
        edges = new HashSet<ArrayList<Integer>>();
        path = new ArrayList<Point>();
        points[start.x][start.y] = true;
        search(start.x, start.y);
        System.out.println(solution == null ? "Solution not found" : "Length of path: " + path.size());
        return solution;
    }
    
    public boolean dfs(int x, int y, boolean[][] visited, Set<Integer> colors) {
        if (x < 0 || x == h || y < 0 || y == w) return true;
        if (visited[x][y]) return true;
        visited[x][y] = true;
        if (grid[x][y] > 0) colors.add(grid[x][y]);
        if (colors.size() > 1) return false;
        
        for(int[] d: Utils.delta) {
            int nx = x + d[0];
            int ny = y + d[1];
            ArrayList<Integer> edge = new ArrayList<Integer>(
                    Arrays.asList(Math.min(x, nx), Math.min(y, ny), Math.max(x, nx), Math.max(y, ny)));
            if (!edges.contains(edge))  {
                if (!dfs(nx, ny, visited, colors)) return false;
            }
        }
        return true;
    }
    public boolean search(int x, int y) {
        if (x == end.x && y == end.y) {

            boolean[][] visited = new boolean[h][w];
//            System.out.println(path.size() + " " + edges.size() + " " + edges.toString());
//            System.out.println("-====");
            for(int i = 0; i < h; i++)
                for(int j = 0; j < w; j++)
                    if (!visited[i][j]) {
                        Set<Integer> colors = new HashSet<Integer>();
                        if (!dfs(i, j, visited, colors)) return false;
                    }
            solution = path;
            return true;
        }

        for(int[] d: Utils.delta) {
            int nx = x + d[0];
            int ny = y + d[1];

            if (nx < 0 || nx == h + 1 || ny < 0 || ny == w + 1) continue;
            if (points[nx][ny]) continue;
            points[nx][ny] = true;
            ArrayList<Integer> edge;
            if (x == nx) {
                edge = new ArrayList<Integer>(Arrays.asList(nx - 1, Math.min(y, ny), nx, Math.min(y, ny)));
            } else {
                edge = new ArrayList<Integer>(Arrays.asList(Math.min(x, nx), ny - 1, Math.min(x, nx), ny));
            }
            edges.add(edge);
            path.add(new Point(d[1], d[0]));
            if (search(nx, ny)) return true;
            edges.remove(edge);
            points[nx][ny] = false;
            path.remove(path.size() - 1);


        }

        return false;
    }
}


enum PuzzleType {COLOR_PUZZLE, TRIANGLE_PUZZLE}

class Puzzle {
    public int[][] gridValues;

    public Puzzle(int[][] gridValues) {
        this.gridValues = gridValues;
    }
}


class PuzzleDataExtractor {
    private BufferedImage image;
    private BufferedImage colorImage;

    private Region region;
    private Random random;

    private int[] puzzleBoundingBox;
    private Point[][] centers;


    public PuzzleDataExtractor(BufferedImage image, BufferedImage colorImage, Region region) {
        this.image = image;
        this.colorImage = colorImage;
        //region of puzzle grid
        this.region = region;

        random = new Random();
        puzzleBoundingBox = getPuzzleBoundingBox();
        centers = getCenters();
    }

    public PuzzleType getPuzzleType() {
        return region.color < 60 ? PuzzleType.COLOR_PUZZLE : PuzzleType.TRIANGLE_PUZZLE;
    }

    private int[] getPuzzleBoundingBox() {
        return puzzleBoundingBox == null ?
                new int[]{region.leftx, region.rightx, region.topy, region.bottomy} : puzzleBoundingBox;
    }

    private void drawCross(Graphics2D graphics, int x, int y) {
        drawCross(graphics, x, y, 20);
    }

    private void drawCross(Graphics2D graphics, int x, int y, int len) {
        graphics.drawLine(x - len / 2, y, x + len / 2, y);
        graphics.drawLine(x, y - len / 2, x, y + len / 2);
    }

    private int getTrianglesCount(Point p) {
        int samples = 1000;
        int[] puzzleBoundingBox = getPuzzleBoundingBox();
        int cellSize = (puzzleBoundingBox[1] - puzzleBoundingBox[0]) / (5 * 2);
        int gray = 0;
        for(int i = 0; i < samples; i++) {
            int nx = Utils.nextInt(random, p.x - cellSize, p.x + cellSize);
            int ny = Utils.nextInt(random, p.y - cellSize/3, p.y + cellSize/3);

            gray += Utils.getGS(image, nx, ny) - 13;
        }

        gray /= samples;
//        System.out.println(gray);

        if (gray < 10)
            gray = 0;
        else if (gray < 50)
            gray = 1;
        else if (gray < 78)
            gray = 2;
        else
            gray = 3;
        return gray;
    }
    private int getColor(Point p) {
        int samples = 100;
        int gap = 5;
        int gray = 0;
        int[] rgb = new int[]{0, 0, 0};
        for(int i = 0; i < samples; i++) {
            int nx = Utils.nextInt(random, p.x - gap, p.x + gap);
            int ny = Utils.nextInt(random, p.y - gap, p.y + gap);
            
            gray += Utils.getGS(image, nx, ny);
            int c = colorImage.getRGB(nx, ny);
            rgb[0] += c>>16&0x000000FF;
            rgb[1] += c>>8&0x000000FF;
            rgb[2] += c&0x000000FF;
        }
        rgb[0] /= samples; rgb[1] /= samples; rgb[2] /= samples;
        gray /= samples;

        int[][] colors = new int[][]{
                {16, 151, 122},
                {25, 56, 44},
                {200, 196, 189},
                {139, 143, 75},
                {141, 24, 173}};


        int minDist = Integer.MAX_VALUE, minIdx = 0;
        for(int i = 0; i < colors.length; i++) {
            int dist = Utils.dist(colors[i], rgb);
            if (dist < minDist) {
                minDist = dist;
                minIdx = i;
            }
        }
//        System.out.println(gray + " " + rgb[0] + " " + rgb[1] + " " + rgb[2]);

        return minIdx;
    }
    private int[] getLeftRightBorders(int y) {
        int l = Integer.MAX_VALUE, r = Integer.MIN_VALUE;
        for(Point p: region.points) {
            if (p.y == y) {
                l = Math.min(l, p.x);
                r = Math.max(r, p.x);
            }
        }
        return new int[]{l, r};
    }
    private int[] getMiddle4(int l, int r) {
        final double CELL_RATIO = 5;
        int cellSize = (int) ((1.0 * r - l) / CELL_RATIO);
        int gapSize = ((r - l) - 4 * cellSize) / 5;
        return new int[]{l + gapSize + cellSize / 2,
                l + 2 * gapSize + cellSize + cellSize / 2,
                l + 3 * gapSize + 2 * cellSize + cellSize / 2,
                l + 4 * gapSize + 3 * cellSize + cellSize / 2
        };
    }
    private Point[][] getCenters() {
        if (centers != null) return centers;

        int[] bb = getPuzzleBoundingBox();
        int[] middle4 = getMiddle4(bb[2], bb[3]);
        Point[][] centers = new Point[4][4];
        for(int i = 0; i < 4; i++) {
            int[] lr = getLeftRightBorders(middle4[0]);
            int[] middle4lr = getMiddle4(lr[0], lr[1]);
            for(int j = 0; j < 4; j++) {
                centers[i][j] = new Point(middle4lr[j], middle4[i]);
            }
        }
        return centers;
    }

    public Puzzle getColorPuzzle() {
        Point[][] centers = getCenters();
        int[][] values = new int[4][4];
        for (int i = 0; i < values.length; i++)
            for(int j = 0; j < values.length; j++) {
                values[i][j] = getColor(centers[i][j]);
            }
        return new Puzzle(values);
    }
    public Puzzle getTrianglesPuzzle() {
        Point[][] centers = getCenters();
        int[][] values = new int[4][4];
        for (int i = 0; i < values.length; i++)
            for(int j = 0; j < values.length; j++) {
                values[i][j] = getTrianglesCount(centers[i][j]);
            }
        return new Puzzle(values);
    }
    public void drawPuzzleCellCenters(BufferedImage image) {
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.RED);
        Point[][] centers = getCenters();
        for(int i = 0; i < 4; i++)
            for(int j = 0; j < 4; j++)
                drawCross(graphics, centers[i][j].x, centers[i][j].y);
    }

    public void drawPuzzleBoundingBox(BufferedImage image) {
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.RED);

        int[] bb = getPuzzleBoundingBox() ;
        graphics.drawLine(bb[0], bb[2], bb[0], bb[3]);
        graphics.drawLine(bb[0], bb[2], bb[1], bb[2]);
        graphics.drawLine(bb[0], bb[3], bb[1], bb[3]);
        graphics.drawLine(bb[1], bb[2], bb[1], bb[3]);
    }
    
    public void drawPath(BufferedImage image, ArrayList<Point> path) {
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.GREEN);
        graphics.setStroke(new BasicStroke(6));
        int[] bb = getPuzzleBoundingBox();
        if (path == null) {
            graphics.drawLine(bb[0], bb[3], bb[1], bb[2]);
            graphics.drawLine(bb[0], bb[2], bb[1], bb[3]);
            return;
        }
        int CELL_RATIO = 5;
        int cellSize = (int) ((1.0 * puzzleBoundingBox[1] - puzzleBoundingBox[0]) / CELL_RATIO);
        int gapSize = ((puzzleBoundingBox[1] - puzzleBoundingBox[0]) - 4 * cellSize) / 5;
        Point prev = new Point(region.leftx + gapSize / 2, region.bottomy - gapSize / 2);
        for(Point next: path) {
            Point nextPoint = new Point(prev.x + next.x * (cellSize + gapSize), prev.y + next.y * (cellSize + gapSize));
            graphics.drawLine(prev.x, prev.y, nextPoint.x, nextPoint.y);
            prev = nextPoint;
        }
    }
}



class Region {
    public int id;
    public int color;
    public int minx = Integer.MAX_VALUE, miny = Integer.MAX_VALUE;
    public int maxx = Integer.MIN_VALUE, maxy = Integer.MIN_VALUE;
    public int leftx = Integer.MAX_VALUE, rightx = Integer.MIN_VALUE;
    public int bottomy = Integer.MIN_VALUE, topy = Integer.MAX_VALUE;

    public boolean stats = false;
    public int w, h, wCenter, hCenter;
    public int bbArea;
    public double bbRatio, whRatio, whRatioCenter;

    ArrayList<Point> points = new ArrayList<Point>();

    public Region(int id) {
        this.id = id;
    }
    public Region(int id, int color) {
        this.id = id;
        this.color = color;
    }

    public void addPoint(int x, int y) {
        this.points.add(new Point(x, y));
    }

    public void calcStats() {
        if (stats) return;
        stats = true;
        for(Point p: points) {
            minx = Math.min(minx, p.x); maxx = Math.max(maxx, p.x);
            miny = Math.min(miny, p.y); maxy = Math.max(maxy, p.y);

        }
        w = maxx - minx;
        h = maxy - miny;

        bbArea = w * h;
        bbRatio = 1.0 * points.size() / bbArea;
        whRatio = (h > 0) ? (1.0 * w / h) : 0.0;

        int centerX = (maxx + minx) / 2;
        int centerY = (maxy + miny) / 2;

        for(Point p: points) {
            if (p.y == centerY) {
                leftx = Math.min(leftx, p.x);
                rightx = Math.max(rightx, p.x);
            }

            if (p.x == centerX) {
                topy = Math.min(topy, p.y);
                bottomy = Math.max(bottomy, p.y);
            }
        }

        wCenter = rightx - leftx;
        hCenter = bottomy - topy;
        whRatioCenter = (hCenter > 0) ? 1.0 * wCenter / hCenter : 0;
    }

    public void printStats() {
        calcStats();
        System.out.println(color + " " + points.size() + " " + bbRatio + " " + whRatio + " " + whRatioCenter);
    }

    public boolean isPuzzle() {
        calcStats();
        return (color > 30 && points.size() > 100 &&
                w > 150 && h > 150 &&
                bbRatio > 0.35 && bbRatio < 0.5 &&
                whRatio > 0.8 && whRatio < 1.25
                && whRatioCenter > 0.8 && whRatioCenter < 1.25);
    }

    public BufferedImage getImage(int w, int h) {
        BufferedImage result = new BufferedImage(w, h,
                BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y++) {
                Utils.setGS(result, x, y, 255);
            }
        for (Point p : points) {
            Utils.setGS(result, p.x, p.y, 0);
        }
        return result;
    }
}


class Utils {

    public static int[][] delta = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

    public static void dfs(BufferedImage image, int[][] region, int x, int y, int regionID) {
        region[x][y] = regionID;

        for(int i = 0; i < 4; i++) {
            int nx = x + delta[i][0];
            if (nx < 0 || nx == region.length) continue;
            int ny = y + delta[i][1];
            if (ny < 0 || ny == region[0].length) continue;
            if (region[nx][ny] > -1 || Math.abs(Utils.getGS(image, x, y) - Utils.getGS(image, nx, ny)) > 1) continue;

            dfs(image, region, nx, ny, regionID);
        }
    }

    public static void saveImage(String filename, BufferedImage image) {
        File file = new File(filename);
        try {
            ImageIO.write(image, "png", file);
        } catch (Exception e) {
            System.out.println(e.toString()+" Image '"+filename
                    +"' saving failed.");
        }
    }
    
    public static BufferedImage getGrayScale(BufferedImage image) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < image.getWidth(); ++x)
            for (int y = 0; y < image.getHeight(); ++y)
            {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb & 0xFF);
                int grayLevel =(int) (0.2126 * r + 0.7152 * g + 0.0722 * b);
                int gray = (grayLevel << 16) + (grayLevel << 8) + grayLevel;
                result.setRGB(x, y, gray);
            }
        return result;
    }

    public static int getGS(BufferedImage image, int x, int y) {
        return image.getRGB(x,y) & 0xFF;
    }

    public static void setGS(BufferedImage image, int x, int y, int val) {
        image.setRGB(x, y, val | (val << 8) | (val << 16));
    }

    public static int nextInt(Random random, int l, int r) {
        return random.nextInt((r - l) + 1) + l;
    }
    
    public static int dist(int[] a, int[] b) {
        int result = 0;
        for(int i = 0; i < a.length; i++) 
            result += Math.abs(a[i] - b[i]) * Math.abs(a[i] - b[i]);
        return result;
    }
}

