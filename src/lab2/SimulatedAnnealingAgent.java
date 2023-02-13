package lab2;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class SimulatedAnnealingAgent extends Agent {
    
	public class Coordinate extends Object
	{
		private int x, y;
		public Coordinate(int x, int y)
		{
			this.x = x;
			this.y = y;
		}

		public int getXPosition()
		{
			return this.x;
		}
		
		public int getYPosition()
		{
			return this.y;
		}

		@Override
		public int hashCode()
		{
			return this.x * this.x * this.y * this.y * this.y;
		}
		
		@Override
		public String toString()
		{
			return "(" + this.getXPosition() + ", " + this.getYPosition() + ")";
		}

		@Override
	    public boolean equals(Object other)
        {
        	if(other instanceof Coordinate)
        	{
        		Coordinate b = (Coordinate)other;
        		return this.x == b.x && this.y == b.y;
        	}
        	return false;
	    }
	}

    int footmanID, townhallID; 			// the IDs of our footman and enemy units.
    Coordinate currentDesiredPosition; 	// where do we currently want to go?
    boolean isStuck;					// are we stuck?
    Set<Coordinate> obstacleLocations;	// static obstacles in the map
    int time;							// how many times has simulated annealing suggested a move?
    									// note: DIFFERENT from steps in the game bc game lags
    static double EPSILON = 1e-6;		// how close to zero should temp be before we give up?

    public SimulatedAnnealingAgent(int playernum)
    {
        super(playernum);
        this.footmanID = -1;
        this.townhallID = -1;
        this.currentDesiredPosition = null;
        this.isStuck = false;
        this.obstacleLocations = new HashSet<Coordinate>();
        this.time = 0;

        System.out.println("Constructed SimulatedAnnealingAgent");
    }

    // getters and setters
    public int getFootmanID()
    {
    	return this.footmanID;
    }
    
    public void setFootmanID(int i)
    {
    	this.footmanID = i;
    }
    
    public int getEnemyTownhallID()
    {
    	return this.townhallID;
    }
    
    public void setEnemeyTownhallID(int i)
    {
    	this.townhallID = i;
    }
    
    public Coordinate getCurrentDesiredPosition()
    {
    	return this.currentDesiredPosition;
    }
    
    public void setCurrentDesiredPosition(Coordinate c)
    {
    	this.currentDesiredPosition = c;
    }
    
    public Set<Coordinate> getObstacles()
    {
    	return this.obstacleLocations;
    }
    
    public void setObstacles(Set<Coordinate> obstacles)
    {
    	this.obstacleLocations = obstacles;
    }
    
    public boolean getIsStuck()
    {
    	return this.isStuck;
    }
    
    public void setIsStuck(boolean b)
    {
    	this.isStuck = b;
    }
    
    public int getTime()
    {
    	return this.time;
    }

    public void setTime(int time)
    {
    	this.time = time;
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
    	// get the footman location
        List<Integer> unitIDs = newstate.getUnitIds(this.getPlayerNumber());

        if(unitIDs.size() == 0)
        {
            System.err.println("No units found!");
            return null;
        }

        this.setFootmanID(unitIDs.get(0)); // we only control a single agent
        
        // double check that this is a footman
        if(!newstate.getUnit(this.getFootmanID()).getTemplateView().getName().equals("Footman"))
        {
            System.err.println("Footman unit not found");
            return null;
        }

        // find the enemy playernum
        Integer[] playerNums = newstate.getPlayerNumbers();
        int enemyPlayerNum = -1;
        for(Integer playerNum : playerNums)
        {
            if(playerNum != this.getPlayerNumber()) {
                enemyPlayerNum = playerNum;
                break;
            }
        }

        if(enemyPlayerNum == -1)
        {
            System.err.println("Failed to get enemy player number");
            return null;
        }

        // find the townhall ID
        List<Integer> enemyUnitIDs = newstate.getUnitIds(enemyPlayerNum);

        if(enemyUnitIDs.size() == 0)
        {
            System.err.println("Failed to find enemy units");
            return null;
        }

        for(Integer unitID : enemyUnitIDs)
        {
            Unit.UnitView tempUnit = newstate.getUnit(unitID);
            String unitType = tempUnit.getTemplateView().getName().toLowerCase();
            if(unitType.equals("townhall"))
            {
                this.setEnemeyTownhallID(unitID);
            }
            else
            {
                System.err.println("Unknown unit type");
            }
        }

        if(this.getEnemyTownhallID() == -1)
        {
            System.err.println("Error: Couldn't find townhall");
            return null;
        }

        // get resource locations
        List<Integer> resourceIDs = newstate.getAllResourceIds();
        Set<Coordinate> obstacles = new HashSet<Coordinate>();
        for(Integer resourceID : resourceIDs)
        {
            ResourceNode.ResourceView resource = newstate.getResourceNode(resourceID);
            obstacles.add(new Coordinate(resource.getXPosition(), resource.getYPosition()));
        }
        this.setObstacles(obstacles);
        
        return this.middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory)
    {
        
    	Map<Integer, Action> actions = new HashMap<Integer, Action>();

        Unit.UnitView footmanUnit = newstate.getUnit(this.getFootmanID());
        Unit.UnitView townhallUnit = newstate.getUnit(this.getEnemyTownhallID());

        if(this.getIsStuck())
        {
        	// agent will kill itself
        	actions.put(this.getFootmanID(), Action.createPrimitiveAttack(this.getFootmanID(),
        			this.getFootmanID()));
        }
        else if(townhallUnit != null) // townhall is still alive
        {

	        int footmanX = footmanUnit.getXPosition();
	        int footmanY = footmanUnit.getYPosition();

	        // if we're adjacent to the townhall attack it!
	        if(Math.abs(footmanX - townhallUnit.getXPosition()) <= 1 &&
                    Math.abs(footmanY - townhallUnit.getYPosition()) <= 1)
            {
                System.out.println("Attacking TownHall");
                // if no more movements in the planned path then attack
                actions.put(this.getFootmanID(), Action.createPrimitiveAttack(this.getFootmanID(),
                		this.getEnemyTownhallID()));
            } else //not adjacent to townhall and townhall is still alive...move there
            {
            	if(this.getCurrentDesiredPosition() != null)
            	{
            		// if we're at that location then erase it
            		if(footmanX == this.getCurrentDesiredPosition().getXPosition() &&
            				footmanY == this.getCurrentDesiredPosition().getYPosition())
            		{
            			this.setCurrentDesiredPosition(null);
            		} else
            		{
            			// go there
            			int xDiff = this.getCurrentDesiredPosition().getXPosition() - footmanX;
			            int yDiff = this.getCurrentDesiredPosition().getYPosition() - footmanY;
			
			            // figure out the direction the footman needs to move in
			            Direction nextDirection = getNextDirection(xDiff, yDiff);
			            // System.out.println("creating primitive action to move in direction " + nextDirection);
			
			            actions.put(this.getFootmanID(), Action.createPrimitiveMove(this.getFootmanID(),
			            		nextDirection));
            		}
            	}

            	if(this.getCurrentDesiredPosition() == null)
            	{
			        
			
			        this.setCurrentDesiredPosition(this.getNextPosition(new Coordinate(footmanX, footmanY),
			        		new Coordinate(townhallUnit.getXPosition(),
			        				townhallUnit.getYPosition()),
			        		newstate.getXExtent(),
			        		newstate.getYExtent(),
			        		this.getObstacles()));
			        if(this.getCurrentDesiredPosition() != null)
			        {
			        	System.out.println("moving to " + this.getCurrentDesiredPosition());
			        }
			        if(this.getCurrentDesiredPosition() != null
			        		&& (footmanX != this.getCurrentDesiredPosition().getXPosition()
			        		    || footmanY != this.getCurrentDesiredPosition().getYPosition()))
			        {
			            int xDiff = this.getCurrentDesiredPosition().getXPosition() - footmanX;
			            int yDiff = this.getCurrentDesiredPosition().getYPosition() - footmanY;
			
			            // figure out the direction the footman needs to move in
			            Direction nextDirection = getNextDirection(xDiff, yDiff);
			            // System.out.println("creating primitive action to move in direction " + nextDirection);
			
			            actions.put(this.getFootmanID(), Action.createPrimitiveMove(this.getFootmanID(),
			            		nextDirection));
			        }
            	}
            }
        } else
        {
        	this.terminalStep(newstate, statehistory);
        }

        return actions;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory)
    {
    }

    /**
     * A method to compute the temperature from time. This method is part of simulated annealing.
     * Feel free to play around with it. I chose to convert time to temperature using the equation
     * e^(-0.000000001 * time) which should have temperature decay super slowly.
     * @param time. Discrete time
     * @return temperature to use in simulated annealing
     */
    private double getTemperature(int time)
    {
    	return Math.exp(-1e-9*time); // for instance
    }
    
	/**
     * TODO: calculate the coordinate of the square to move to. Our code will automatically take control
     * 	     once you are adjacent to the townhall and will kill it.
     * @param pt: the current position
     * @param goalPt: the desired position (position of townhall)
     * @param xExent: the max X coordinate of the map
     * @param yExtent: the max Y coordinate of the map
     * @param obstacles: set of obstacles. These obstacles prevent you from occupying squares in the map.
     * @return
     */
    private Coordinate getNextPosition(Coordinate pt, Coordinate goalPt,
    		int xExtent, int yExtent, Set<Coordinate> obstacles)
    {
    	return pt;
    }

    @Override
    public void savePlayerData(OutputStream os)
    {

    }

    @Override
    public void loadPlayerData(InputStream is)
    {

    }
    
    /**
     * A method to get a list of MapPositions representing the neighbors of a node that are valid
     * nodes to move to (valid nodes cannot be occupied by other objects such as trees,...).
     * @param pt The node to find the neighbors of.
     * @param goal The goal node, used to calculate h(n) from any neighbor.
     * @param xExtent The maximum x boundary of the environment (assuming 0 is min).
     * @param yExtent The maximum y boundary of the environment (assuming 0 is min).
     * @param obstacles A set of obstacles in the environment represented as a set of positions
     * that are already occupied.
     * @return ArrayList of valid positions representing unoccupied neighbors of node pt.
     */
    private ArrayList<Coordinate> getAndCheckNeighbors(Coordinate pt, Coordinate goal, int xExtent,
			   int yExtent, Set<Coordinate> obstacles)
	{
	
		//at max 8 neighbors due to diagonal movement
		ArrayList<Coordinate> neighbors = new ArrayList<Coordinate>(8);
		
		// System.out.println("pt=" + pt);
		// for(Coordinate ob: obstacles)
		// {
		// 	System.out.println("obstacle at " + ob);
		// }
		
		//variable to store a neighbor node
		Coordinate newPt = null;
		
		for(int i = pt.getXPosition() - 1; i <= pt.getXPosition() + 1; i++)
		{ //run from pt.x-1 -> pt.x+1
			for(int j = pt.getYPosition() - 1; j <= pt.getYPosition() + 1; j++)
			{ //run from pt.y-1 -> pt.y+1
				if(i != pt.getXPosition() || j != pt.getYPosition())
				{ //if we aren't at (pt.x, pt.y)
					newPt = new Coordinate(i, j); //neighbor point
					// System.out.println("newPt=" + newPt);
					
					//if the point is valid (not occupied and in range)
					if(newPt.getXPosition() >= 0 && newPt.getXPosition() < xExtent
							&& newPt.getYPosition() >= 0 && newPt.getYPosition() < yExtent
							&& !obstacles.contains(newPt))
					{
						neighbors.add(newPt); //add the point to the neighbors list
					}
				}
			}
		}
		
		// for(Coordinate x: neighbors)
		// {
		// 	System.out.println("neighbor " + x);
		// }
		return neighbors;
	}
    
    /**
     * A method to calculate the heuristic from any position to the goal node. This uses the
     * Chebyshev distance D(x, y) = max(|x0 - x1|, |y0 -y1|).
     * @param x the x position of the node.
     * @param y the y position of the node.
     * @param goal the goal node position.
     * @return the Chebyshev distance from (x, y) -> (goal.x, goal.y)
     */
    private float heuristic(int x,int y, Coordinate goal)
    { 
    	return Math.max(Math.abs(x - goal.getXPosition()), Math.abs(y - goal.getYPosition()));
    }

    /**
     * Primitive actions take a direction (e.g. NORTH, NORTHEAST, etc)
     * This converts the difference between the current position and the
     * desired position to a direction.
     *
     * @param xDiff Integer equal to 1, 0 or -1
     * @param yDiff Integer equal to 1, 0 or -1
     * @return A Direction instance (e.g. SOUTHWEST) or null in the case of error
     */
    private Direction getNextDirection(int xDiff, int yDiff) {

        // figure out the direction the footman needs to move in
        if(xDiff == 1 && yDiff == 1)
        {
            return Direction.SOUTHEAST;
        }
        else if(xDiff == 1 && yDiff == 0)
        {
            return Direction.EAST;
        }
        else if(xDiff == 1 && yDiff == -1)
        {
            return Direction.NORTHEAST;
        }
        else if(xDiff == 0 && yDiff == 1)
        {
            return Direction.SOUTH;
        }
        else if(xDiff == 0 && yDiff == -1)
        {
            return Direction.NORTH;
        }
        else if(xDiff == -1 && yDiff == 1)
        {
            return Direction.SOUTHWEST;
        }
        else if(xDiff == -1 && yDiff == 0)
        {
            return Direction.WEST;
        }
        else if(xDiff == -1 && yDiff == -1)
        {
            return Direction.NORTHWEST;
        }

        System.err.println("Invalid path. Could not determine direction");
        return null;
    }
}
