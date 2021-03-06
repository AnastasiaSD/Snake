package SnakeCore;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class GameState {
    private SnakeFactory snakes;
    private char[][] maze;
    private char[][] map;
    private Random rnd;
    private boolean isAlive;
    private List<IObject> objs = new LinkedList<IObject>();
    private int height;
    private int width;

    private HashMap<String, IObjFactory> dic;
    {
        dic = new HashMap<String, IObjFactory>();
        dic.put("Snake", new SnakeFactory());
        dic.put("Food", new FoodFactory());
        //dic.put("Hedg", new HedgFactory());
        //dic.put("Teleport", new TeleportFactory());
        //dic.put("Pillow", new PillowFactory());
    }

    public GameState(char[][] maze,
            ArrayList<Tuple<String, Integer[]>> objsCreators) {
        isAlive = true;
        this.maze = maze;
        map = new char[maze.length][];
        rnd = new Random();
        height = maze.length;
        width = maze[0].length;// bad
        snakes = (SnakeFactory) dic.get("Snake");
        for (Tuple<String, Integer[]> tup : objsCreators) {
                setObjs(dic.get(tup.getX()).configure(this, tup.getY()));
        }
    }


    public char[][] getMap() {
        for (int i = 0; i < map.length; i++)
            map[i] = maze[i].clone();
        /*
        for (Snake snake:snakes.getProducts()) {
            for (int i = 0; i < snake.getBody().size(); i++) {
                Point p = snake.getBody().get(i);
                map[p.y][p.x] = '@';
            }
        }
        *///TODO

        for (int i = 0; i < getObjsArr().size(); i++) {
            Point[] ps = getObjsArr().get(i).getLocs();
            char ico = getObjsArr().get(i).getIcon();
            for (int j = 0; j < ps.length; j++) {
                Point p = ps[j];// �������� �� �����������
                map[p.y][p.x] = ico;
            }
        }
        return map;
    }


    public boolean makeTick() {
        if (!isAlive)
            return false;
        tickFactorys();
        for(Snake snake:snakes.getProducts()) {
            if (!snake.isMoving() || 
                !snake.isAlive() || 
                killCrashed(snake)) {
                continue;
            }
            collise(snake);
        }
        cleanSnakes();
        tickObjs();
        return true;
    } 
    /*
    public Point collise(Snake snake) {
        snake.setNext(getBoundedCord(snake.getNext()));
        IObject col = objsCollision(snake.getNext());
        for (Point nextTmp = null; 
                nextTmp==null || 
                        (nextTmp.x != snake.getNext().x && 
                         nextTmp.y != snake.getNext().y);) {
            if (col == null) {
                break;
            }
            nextTmp = (Point) snake.getNext().clone();
            if (col.interact(snake, snake.getNext())) { //TODO make interact better
                snake.die();
                return null;
            } else {
                snake.setNext(getBoundedCord(snake.getNext()));
                col = objsCollision(snake.getNext());
            }
        }
        if(col!=null) {
            setObjs(col.getFact().utilize(col));//TODO Make it better
            objs.remove(col);
        }
        snake.setNext(getBoundedCord(snake.getNext()));
        return snake.getNext();
    }
    */
    public void collise(Snake snake) {
        snake.setNext(getBoundedCord(snake.getNext()));
        IObject col = objsCollision(snake.getNext());
        if (col == null) {
            if (maze[snake.getNext().y][snake.getNext().x] != '.') {//maze[next.y][next.x] == '+' || //TODO
                snake.die();
            }        
        }else if (col.interact(snake, snake.getNext())) { //TODO make interact better
            snake.die();
        } else {
            setObjs(col.getFact().utilize(col));//TODO Make it better
            objs.remove(col);
        }
    }

    private boolean killCrashed(Snake snake1) {
        for(Snake snake2:snakes.getProducts()) {
            if(snake1!=snake2 && 
               snake1.getNext().x==snake2.getNext().x && 
               snake1.getNext().y==snake2.getNext().y && 
               snake2.isMoving()) {
                snake1.die();
                snake2.die();
                return true;
            }
        }
        return false;
    }
    
    private void cleanSnakes() {
        for(int i=0;i<snakes.getProducts().length;i++) {
            if(!snakes.getProducts()[i].isAlive()) {
                decay(snakes.getProducts()[i]);
            }
        }
    }
    
    private void tickFactorys() {
        for (IObjFactory fact : dic.values())
            setObjs(fact.tick());
    }

    private void tickObjs() {
        for (IObject obj : objs)
            obj.tick();
    }
    /*
    private void tickSnakes() {
        for(Snake snake :snakes.getProducts()) {
            snake.tick();
            if(!snake.isAlive()) {
                decay(snake);
            }
        }
    }
    */

    public Point getBoundedCord(Point p) {
        if (!(p.x >= 0 && p.x < width && p.y >= 0 && p.y < height)) {
            p = new Point((p.x + width) % width, (p.y + height) % height);
        }
        return p;
    }

    private IObject objsCollision(Point p) {
        for (IObject obj : getObjsArr())
            for (Point el : obj.getLocs())
                if (p.x == el.x && p.y == el.y)
                    return obj;
        return null;
    }

    protected char getCell(Point p) {
        if (maze[p.y][p.x] == '#' || maze[p.y][p.x] == '+')
            return '#';
        //if (maze[p.y][p.x] == '+') //TODO
        //    return '+';
        for (Snake s:snakes.getProducts())
            for (Point part : s.getBody())
                if (part.x == p.x && part.y == p.y)
                    return '@';
        IObject obj = objsCollision(p);
        if (obj != null)
            return obj.getIcon();
        return '.';
    }
    
    public ControlIntellect[] getCtrlIntel() {
        return snakes.getCtrlIntel();
    }

    protected Point getRndFreePoint() {
        Point loc = new Point(rnd.nextInt(width), rnd.nextInt(height));
        while (getCell(loc) != '.')
            loc = new Point(rnd.nextInt(width), rnd.nextInt(height));
        return loc;
    }

    protected List<IObject> getObjsArr() {
        return objs;
    }

    public Point getSize() {
        return new Point(width, height);
    }

    public void setObjs(IObject[] objs) {
        if (objs == null)
            return;
        for (IObject obj : objs) {
            this.objs.add(obj);
        }
    }
    public void setObj(IObject obj) {
        this.objs.add(obj);
    }
    public IObjFactory getFact(String s) {
        return dic.get(s);
    }
    public void decay(Snake s) {
        setObjs(snakes.utilize(s));
        objs.remove(s);
    }
    public boolean isSafe(Point p) {
        char ico =getCell(p);
        return ico =='.' || ico=='*';
    }
    public boolean isSafeSafe(Point p) {
        return isSafe(p) && 
               (isSafe(getBoundedCord(new Point(p.x+1,p.y)))||
                isSafe(getBoundedCord(new Point(p.x-1,p.y)))||
                isSafe(getBoundedCord(new Point(p.x,p.y+1)))||
                isSafe(getBoundedCord(new Point(p.x,p.y-1))));
    }
    /*
     * private void setObjs(IObject obj) { this.objs.add(obj); }
     */
    public Snake[] getSnake() {
    	return snakes.getProducts();
    }
    public void setIsAlive(boolean b) {
    	isAlive = b;
    }
}


